package uk.co.gresearch.siembol.parsers.application.factory;

import org.adrianwalker.multilinestring.Multiline;
import org.junit.Assert;
import org.junit.Test;

public class ParsingApplicationFactoryImplTest {
    private final ParsingApplicationFactory factory;
    public ParsingApplicationFactoryImplTest() throws Exception {
        factory = new ParsingApplicationFactoryImpl();
    }

    /**
     *{
     *   "parsing_app_name": "test",
     *   "parsing_app_version": 1,
     *   "parsing_app_author": "dummy",
     *   "parsing_app_description": "Description of parser application",
     *   "parsing_app_settings": {
     *     "input_topics": [
     *       "secret"
     *     ],
     *     "error_topic": "error",
     *     "input_parallelism": 1,
     *     "parsing_parallelism": 2,
     *     "output_parallelism": 3,
     *     "parsing_app_type": "single_parser"
     *   },
     *   "parsing_settings": {
     *     "single_parser": {
     *       "parser_name": "single",
     *       "output_topic": "output"
     *     }
     *   }
     * }
     **/
    @Multiline
    public static String simpleSingleApplicationParser;


    /**
     * {
     *   "parsing_app_name": "test",
     *   "parsing_app_version": 1,
     *   "parsing_app_author": "dummy",
     *   "parsing_app_description": "Description of parser application",
     *   "parsing_app_settings": {
     *     "input_topics": [
     *       "secret"
     *     ],
     *     "error_topic": "error",
     *     "input_parallelism": 1,
     *     "parsing_parallelism": 2,
     *     "output_parallelism": 3,
     *     "parsing_app_type": "router_parsing"
     *   },
     *   "parsing_settings": {
     *     "routing_parser": {
     *       "router_parser_name": "router",
     *       "routing_field": "host",
     *       "routing_message": "msg",
     *       "merged_fields": [
     *         "timestamp",
     *         "syslog_host"
     *       ],
     *       "default_parser": {
     *         "parser_name": "default",
     *         "output_topic": "output_default"
     *       },
     *       "parsers": [
     *       {
     *         "routing_field_pattern": "secret",
     *         "parser_properties": {
     *           "parser_name": "single",
     *           "output_topic": "out_secret"
     *         }
     *       }
     *     ]
     *   }
     *  }
     * }
     **/
    @Multiline
    public static String simpleRoutingApplicationParser;


    /**
     * {
     *   "parsers_version": 1,
     *   "parsers_configurations": [
     *     {
     *       "parser_description": "for testing single app parser",
     *       "parser_version": 2,
     *       "parser_name": "single",
     *       "parser_author": "dummy",
     *       "parser_attributes": {
     *         "parser_type": "generic"
     *       }
     *     },
     *     {
     *       "parser_description": "for testing routing app paerser",
     *       "parser_version": 2,
     *       "parser_name": "router",
     *       "parser_author": "dummy",
     *       "parser_attributes": {
     *         "parser_type": "generic"
     *       }
     *     },
     *     {
     *       "parser_description": "for testing routing app parser",
     *       "parser_version": 2,
     *       "parser_name": "default",
     *       "parser_author": "dummy",
     *       "parser_attributes": {
     *         "parser_type": "generic"
     *       }
     *     }
     *   ]
     * }
     **/
    @Multiline
    public static String testParsersConfigs;


    @Test
    public void testGetSchema() {
        ParsingApplicationFactoryResult schemaResult = factory.getSchema();
        Assert.assertTrue(schemaResult.getStatusCode() == ParsingApplicationFactoryResult.StatusCode.OK);
        Assert.assertFalse(schemaResult.getAttributes().getJsonSchema().isEmpty());
    }

    @Test
    public void testValidationSingleGood() {
        ParsingApplicationFactoryResult result = factory.validateConfiguration(simpleSingleApplicationParser);
        Assert.assertTrue(result.getStatusCode() == ParsingApplicationFactoryResult.StatusCode.OK);
    }

    @Test
    public void testValidationSingleFail() {
        ParsingApplicationFactoryResult result = factory.validateConfiguration(simpleSingleApplicationParser
                .replace("error_topic", "dummy"));
        Assert.assertTrue(result.getStatusCode() == ParsingApplicationFactoryResult.StatusCode.ERROR);
        Assert.assertTrue(result.getAttributes().getMessage().contains("error_topic"));
    }

    @Test
    public void testValidationSingleFail2() {
        ParsingApplicationFactoryResult result = factory.validateConfiguration(simpleSingleApplicationParser
                .replace("\"parsing_parallelism\": 2,", ""));
        Assert.assertTrue(result.getStatusCode() == ParsingApplicationFactoryResult.StatusCode.ERROR);
        Assert.assertTrue(result.getAttributes().getMessage()
                .contains("missing required properties ([\"parsing_parallelism\"])"));
    }

    @Test
    public void testCreationSingleGood() {
        ParsingApplicationFactoryResult result = factory.create(simpleSingleApplicationParser, testParsersConfigs);
        Assert.assertTrue(result.getStatusCode() == ParsingApplicationFactoryResult.StatusCode.OK);
        Assert.assertEquals("test", result.getAttributes().getName());
        Assert.assertEquals(1, result.getAttributes().getInputParallelism().intValue());
        Assert.assertEquals(2, result.getAttributes().getParsingParallelism().intValue());
        Assert.assertEquals(3, result.getAttributes().getOutputParallelism().intValue());
        Assert.assertEquals("secret", result.getAttributes().getInputTopics().get(0));
        Assert.assertNotNull(result.getAttributes().getApplicationParser());
    }

    @Test
    public void testCreationSingleWrongApplication() {
        ParsingApplicationFactoryResult result = factory.create(
                simpleSingleApplicationParser.replace("error_topic", "dummy"),
                testParsersConfigs);
        Assert.assertTrue(result.getStatusCode() == ParsingApplicationFactoryResult.StatusCode.ERROR);
        Assert.assertTrue(result.getAttributes().getMessage().contains("error_topic"));
    }

    @Test
    public void testCreationSingleWrongParserConfigs() {
        ParsingApplicationFactoryResult result = factory.create(simpleSingleApplicationParser, "INVALID");
        Assert.assertTrue(result.getStatusCode() == ParsingApplicationFactoryResult.StatusCode.ERROR);
        Assert.assertTrue(result.getAttributes().getMessage().contains("INVALID"));
    }

    @Test
    public void testCreationSingleMissingParserConfigs() {
        ParsingApplicationFactoryResult result = factory.create(simpleSingleApplicationParser,
                testParsersConfigs.replace("single", "unwanted"));
        Assert.assertTrue(result.getStatusCode() == ParsingApplicationFactoryResult.StatusCode.ERROR);
        Assert.assertTrue(result.getAttributes().getMessage().contains("Missing parser: single"));
    }

    @Test
    public void testValidationRoutingGood() {
        ParsingApplicationFactoryResult result = factory.validateConfiguration(simpleRoutingApplicationParser);
        Assert.assertTrue(result.getStatusCode() == ParsingApplicationFactoryResult.StatusCode.OK);
    }

    @Test
    public void testValidationRoutingFail() {
        ParsingApplicationFactoryResult result = factory.validateConfiguration(simpleRoutingApplicationParser
                .replace("error_topic", "dummy"));
        Assert.assertTrue(result.getStatusCode() == ParsingApplicationFactoryResult.StatusCode.ERROR);
        Assert.assertTrue(result.getAttributes().getMessage().contains("error_topic"));
    }

    @Test
    public void testCreationRoutingGood() {
        ParsingApplicationFactoryResult result = factory.create(simpleRoutingApplicationParser, testParsersConfigs);
        Assert.assertTrue(result.getStatusCode() == ParsingApplicationFactoryResult.StatusCode.OK);
        Assert.assertEquals("test", result.getAttributes().getName());
        Assert.assertEquals(1, result.getAttributes().getInputParallelism().intValue());
        Assert.assertEquals(2, result.getAttributes().getParsingParallelism().intValue());
        Assert.assertEquals(3, result.getAttributes().getOutputParallelism().intValue());
        Assert.assertEquals("secret", result.getAttributes().getInputTopics().get(0));
        Assert.assertNotNull(result.getAttributes().getApplicationParser());
    }

    @Test
    public void testCreationRoutingWrongApplication() {
        ParsingApplicationFactoryResult result = factory.create(
                simpleRoutingApplicationParser.replace("error_topic", "dummy"),
                testParsersConfigs);
        Assert.assertTrue(result.getStatusCode() == ParsingApplicationFactoryResult.StatusCode.ERROR);
        Assert.assertTrue(result.getAttributes().getMessage().contains("error_topic"));
    }

    @Test
    public void testCreationRoutingWrongParserConfigs() {
        ParsingApplicationFactoryResult result = factory.create(simpleRoutingApplicationParser, "INVALID");
        Assert.assertTrue(result.getStatusCode() == ParsingApplicationFactoryResult.StatusCode.ERROR);
        Assert.assertTrue(result.getAttributes().getMessage().contains("INVALID"));
    }

    @Test
    public void testCreationRoutingMissingRouterParserConfigs() {
        ParsingApplicationFactoryResult result = factory.create(simpleRoutingApplicationParser,
                testParsersConfigs.replace("router", "unwanted"));
        Assert.assertTrue(result.getStatusCode() == ParsingApplicationFactoryResult.StatusCode.ERROR);
        Assert.assertTrue(result.getAttributes().getMessage().contains("Missing parser: router"));
    }

    @Test
    public void testCreationRoutingMissingDefaultParserConfigs() {
        ParsingApplicationFactoryResult result = factory.create(simpleRoutingApplicationParser,
                testParsersConfigs.replace("default", "unwanted"));
        Assert.assertTrue(result.getStatusCode() == ParsingApplicationFactoryResult.StatusCode.ERROR);
        Assert.assertTrue(result.getAttributes().getMessage().contains("Missing parser: default"));
    }

    @Test
    public void testCreationRoutingMissingParserConfigs() {
        ParsingApplicationFactoryResult result = factory.create(simpleRoutingApplicationParser,
                testParsersConfigs.replace("single", "unwanted"));
        Assert.assertTrue(result.getStatusCode() == ParsingApplicationFactoryResult.StatusCode.ERROR);
        Assert.assertTrue(result.getAttributes().getMessage().contains("Missing parser: single"));
    }
}
