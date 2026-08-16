package bakery.scenarios

import bakery.dns.DnsChange
import bakery.dns.DnsProvider
import bakery.dns.DnsProvisioner
import bakery.dns.DnsReconciliationResult
import bakery.dns.DnsRecord
import bakery.dns.ExistingDnsRecord
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat

/**
 * BDD steps for the DNS provisioning reconciliation (BKY-DNS-5).
 *
 * Pure domain behaviour: an in-memory fake [DnsProvider] stands in for the
 * live zone, [DnsProvisioner] reconciles the desired records against it.
 */
class DnsProvisionSteps {

    private val domain = "talaria.school"
    private var nextId = 1L

    /** In-memory live zone. */
    private val zone = mutableListOf<ExistingDnsRecord>()

    private var desired: List<DnsRecord> = emptyList()
    private var result: DnsReconciliationResult? = null
    private var existingRecordId: Long? = null

    private val provider: DnsProvider = FakeDnsProvider(zone) { nextId++ }
    private val provisioner = DnsProvisioner(provider, domain)

    // --- Given ---

    @Given("a site configuration without a dns section")
    fun aSiteConfigurationWithoutDnsSection() {
        desired = emptyList()
    }

    @Given("a zone without records")
    fun aZoneWithoutRecords() {
        zone.clear()
        nextId = 1L
    }

    @Given("a zone with an existing record")
    fun aZoneWithAnExistingRecord(table: DataTable) {
        zone.clear()
        nextId = 1L
        val record = parseRecords(table).single()
        val id = nextId++
        zone += ExistingDnsRecord(id = id, record = record)
        existingRecordId = id
    }

    @Given("desired records")
    fun desiredRecords(table: DataTable) {
        desired = parseRecords(table)
    }

    // --- When ---

    @When("I reconcile the desired records in dry run")
    fun iReconcileInDryRun() {
        result = provisioner.reconcile(desired, dryRun = true, allowDelete = false)
    }

    @When("I reconcile the desired records again in dry run")
    fun iReconcileAgainInDryRun() {
        result = provisioner.reconcile(desired, dryRun = true, allowDelete = false)
    }

    @When("I reconcile and apply the desired records with delete allowed")
    fun iReconcileAndApplyWithDeleteAllowed() {
        result = provisioner.reconcile(desired, dryRun = false, allowDelete = true)
    }

    // --- Then ---

    @Then("no DNS reconciliation is performed")
    fun noDnsReconciliationIsPerformed() {
        assertThat(result).isNull()
    }

    @Then("the desired records are empty")
    fun theDesiredRecordsAreEmpty() {
        assertThat(desired).isEmpty()
    }

    @Then("the plan is empty")
    fun thePlanIsEmpty() {
        assertThat(result!!.plan).isEmpty()
    }

    @Then("the reconciliation is a no-op")
    fun theReconciliationIsANoOp() {
        assertThat(result!!.noop).isTrue()
    }

    @Then("the plan has {int} create")
    fun thePlanHasCreate(count: Int) {
        assertThat(result!!.plan.filterIsInstance<DnsChange.Create>()).hasSize(count)
    }

    @Then("the plan has {int} creates")
    fun thePlanHasCreates(count: Int) {
        thePlanHasCreate(count)
    }

    @Then("the plan has {int} update")
    fun thePlanHasUpdate(count: Int) {
        assertThat(result!!.plan.filterIsInstance<DnsChange.Update>()).hasSize(count)
    }

    @Then("the plan has {int} updates")
    fun thePlanHasUpdates(count: Int) {
        thePlanHasUpdate(count)
    }

    @Then("the plan has {int} delete")
    fun thePlanHasDelete(count: Int) {
        assertThat(result!!.plan.filterIsInstance<DnsChange.Delete>()).hasSize(count)
    }

    @Then("the plan has {int} deletes")
    fun thePlanHasDeletes(count: Int) {
        thePlanHasDelete(count)
    }

    @Then("the plan has {int} create and {int} delete")
    fun thePlanHasCreatesAndDelete(creates: Int, deletes: Int) {
        thePlanHasCreates(creates)
        thePlanHasDelete(deletes)
    }

    @Then("the created record is {word} {string} pointing to {string}")
    fun theCreatedRecordIs(type: String, name: String, value: String) {
        val create = result!!.plan.filterIsInstance<DnsChange.Create>().single()
        assertThat(create.record.type).isEqualTo(type)
        assertThat(create.record.name).isEqualTo(name)
        assertThat(create.record.value).isEqualTo(value)
    }

    @Then("all creates target apex A records")
    fun allCreatesTargetApexARecords() {
        val creates = result!!.plan.filterIsInstance<DnsChange.Create>()
        assertThat(creates).hasSize(4)
        assertThat(creates.map { it.record.type }).containsOnly("A")
        assertThat(creates.map { it.record.name }).containsOnly("@")
    }

    @Then("the update keeps the existing record id")
    fun theUpdateKeepsTheExistingRecordId() {
        val update = result!!.plan.filterIsInstance<DnsChange.Update>().single()
        assertThat(update.id).isEqualTo(existingRecordId)
        assertThat(update.previous.value).isEqualTo(update.record.value)
    }

    @Then("the delete is skipped in dry run")
    fun theDeleteIsSkippedInDryRun() {
        val delete = result!!.plan.filterIsInstance<DnsChange.Delete>().single()
        assertThat(result!!.skipped).contains(delete)
        assertThat(result!!.applied).doesNotContain(delete)
    }

    @Then("the orphan record is removed from the zone")
    fun theOrphanRecordIsRemovedFromTheZone() {
        val delete = result!!.plan.filterIsInstance<DnsChange.Delete>().single()
        val orphan = delete.record
        assertThat(zone).noneMatch {
            it.record.type == orphan.type &&
                it.record.name == orphan.name &&
                it.record.value == orphan.value
        }
    }

    @Then("the zone matches the desired records exactly")
    fun theZoneMatchesTheDesiredRecordsExactly() {
        assertThat(zone).hasSize(desired.size)
        val zoneSet = zone.map { Triple(it.record.type, it.record.name, it.record.value) }.toSet()
        val desiredSet = desired.map { Triple(it.type, it.name, it.value) }.toSet()
        assertThat(zoneSet).isEqualTo(desiredSet)
    }

    // --- Helpers ---

    private fun parseRecords(table: DataTable): List<DnsRecord> =
        table.asMaps().map { row ->
            DnsRecord(
                type = row.getValue("type"),
                name = row.getValue("name"),
                value = row.getValue("value"),
                ttl = row.getValue("ttl").toInt(),
            )
        }
}

/**
 * In-memory [DnsProvider] standing in for the live zone in BDD tests.
 */
private class FakeDnsProvider(
    private val zone: MutableList<ExistingDnsRecord>,
    private val nextId: () -> Long,
) : DnsProvider {

    override fun listRecords(domain: String): List<ExistingDnsRecord> = zone.toList()

    override fun createRecord(domain: String, record: DnsRecord): Long {
        val id = nextId()
        zone += ExistingDnsRecord(id = id, record = record)
        return id
    }

    override fun updateRecord(domain: String, id: Long, record: DnsRecord) {
        val index = zone.indexOfFirst { it.id == id }
        require(index >= 0) { "No existing record with id $id" }
        zone[index] = ExistingDnsRecord(id = id, record = record)
    }

    override fun deleteRecord(domain: String, id: Long) {
        zone.removeAll { it.id == id }
    }

    override fun refreshZone(domain: String) {
        // no-op for the in-memory zone
    }

    override fun isAvailable(): Boolean = true

    override fun name(): String = "fake"
}