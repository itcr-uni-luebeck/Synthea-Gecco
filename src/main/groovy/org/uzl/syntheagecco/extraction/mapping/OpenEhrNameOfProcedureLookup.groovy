package org.uzl.syntheagecco.extraction.mapping

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.uzl.syntheagecco.openehr.sdk.model.generated.geccoprozedurcomposition.definition.NameDerProzedurDefiningCode
import org.uzl.syntheagecco.utility.FileManipulation

import java.nio.file.Path
import java.nio.file.Paths

class OpenEhrNameOfProcedureLookup extends Lookup<String, List<NameDerProzedurDefiningCode>>{

    private static final Logger logger = LogManager.getLogger(OpenEhrDiagnosisCategoryLookup.class)

    OpenEhrNameOfProcedureLookup(){
        super({
            logger.info("[#]Creating openEhr procedure name lookup ...")

            def indexFile = FileManipulation.getResource(Paths.get("OpenEhrNameOfProcedureIndex.txt"))
            def sources = []
            indexFile.split("\\n").each {fileName ->
                sources << FileManipulation.getResource(Paths.get(fileName))
            }

            def mapper = new ObjectMapper()
            def codeMap = new MultiListMap<String, NameDerProzedurDefiningCode>()

            def diagnosisMap = new HashMap<String, NameDerProzedurDefiningCode>()
            NameDerProzedurDefiningCode.values().each {value ->
                diagnosisMap[value.getCode()] = value
            }

            if(sources.size().is(0)) {
                logger.error("No JSON files in parent directory 'maps\\openehr_category\\procedure_category'!")
                throw new Exception("No JSON files could be found in 'maps\\openehr_category\\procedure_category'")
            }
            else{
                //Read JSON files
                logger.debug("Reading JSON files containing codes...")
                sources.each { mapFile ->
                    try{
                        JsonNode jsonRoot = mapper.readTree(mapFile)
                        def currentCategory = diagnosisMap[jsonRoot.get("code").asText()]
                        JsonNode codes = jsonRoot.get("children")
                        if(!codes.isNull()){
                            if(codes != null && codes.isArray()){
                                for(int i = 0; i < codes.size(); i++){
                                    JsonNode codeEntry = codes.get(i)
                                    codeMap.putOne(codeEntry.get("code").asText(), currentCategory)
                                }
                            }
                            else{
                                throw new Exception("'codes' JSON object in JSON file '${mapFile.getAbsolutePath()}' could not " +
                                        "be converted to JSON array!")
                            }
                        }
                        else{
                            logger.warn("There are no codes for diagnosis name category '${currentCategory.toString()}'.")
                        }
                    }
                    catch (Exception exc){
                        logger.error("An error occurred during the creation of the code lookup!" +
                                "\nMessage:    ${exc.getMessage()}" +
                                "\nStacktrace: ${ExceptionUtils.getStackTrace(exc)}")
                    }
                }
                logger.debug("Total entry count: ${codeMap.size()}")
                return codeMap
            }
        }.call())
    }

}
