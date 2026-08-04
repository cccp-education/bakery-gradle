package bakery.i18n

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import document.translation.validation.PlantUmlValidationReportEntry
import document.translation.validation.TableValidationReportEntry

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ValidationReport(
    val table: List<TableValidationReportEntry>,
    val plantUml: List<PlantUmlValidationReportEntry>,
) {
    fun toJson(): String {
        val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
        return mapper.writeValueAsString(this)
    }
}
