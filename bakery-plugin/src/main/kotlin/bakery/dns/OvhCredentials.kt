package bakery.dns

import bakery.SecretField
import bakery.maskSecret

/**
 * OVH API credentials (EPIC BKY-DNS).
 *
 * Pure DDD domain type. Secrets are never exposed: toString masks every
 * field via [maskSecret], mirroring [bakery.RepositoryCredentials].
 */
data class OvhCredentials(
    /** OVH application key (consumer API). */
    val applicationKey: String = "",
    /** OVH application secret (consumer API). */
    val applicationSecret: String = "",
    /** OVH consumer key (scoped access token). */
    val consumerKey: String = "",
) {
    override fun toString(): String =
        "OvhCredentials(applicationKey='${maskSecret(SecretField.ApiKey(applicationKey))}', " +
            "applicationSecret='${maskSecret(SecretField.Password(applicationSecret))}', " +
            "consumerKey='${maskSecret(SecretField.ApiKey(consumerKey))}')"
}