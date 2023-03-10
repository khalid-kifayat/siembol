package uk.co.gresearch.siembol.response.stream.ruleservice;

import org.adrianwalker.multilinestring.Multiline;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import uk.co.gresearch.siembol.common.model.ZookeeperAttributesDto;
import uk.co.gresearch.siembol.common.zookeper.ZookeeperConnector;
import uk.co.gresearch.siembol.common.zookeper.ZookeeperConnectorFactory;
import uk.co.gresearch.siembol.response.common.*;
import uk.co.gresearch.siembol.response.compiler.RespondingCompiler;
import uk.co.gresearch.siembol.response.compiler.RespondingCompilerImpl;
import uk.co.gresearch.siembol.response.engine.ResponseEngine;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static uk.co.gresearch.siembol.response.common.RespondingResult.StatusCode.OK;
import static uk.co.gresearch.siembol.response.common.ResponseEvaluationResult.MATCH;

public class ZookeeperRulesProviderTest {
    /**
     * {
     *   "rules_version": 111,
     *   "rules": [
     *     {
     *       "rule_name": "default_rule",
     *       "rule_author": "john",
     *       "rule_version": 1,
     *       "rule_description": "default rule",
     *       "evaluators": [
     *         {
     *           "evaluator_type": "fixed_result",
     *           "evaluator_attributes": {
     *             "evaluation_result": "match"
     *           }
     *         }
     *       ]
     *     }
     *   ]
     * }
     */
    @Multiline
    public static String testingRules;
    private RespondingCompiler compiler;
    private MetricFactory testMetricFactory;
    private ZookeeperConnectorFactory zookeeperConnectorFactory;
    private ZookeeperConnector rulesZookeeperConnector;
    private ZookeeperRulesProvider rulesProvider;
    private ZookeeperAttributesDto zookeperAttributes;

    @Before
    public void setUp() throws Exception {
        testMetricFactory = new TestMetricFactory();
        List<RespondingEvaluatorFactory> evaluatorFactories = new ArrayList<>();
        evaluatorFactories.addAll(ProvidedEvaluators.getRespondingEvaluatorFactories()
                .getAttributes()
                .getRespondingEvaluatorFactories());

        compiler = new RespondingCompilerImpl.Builder()
                .addRespondingEvaluatorFactories(evaluatorFactories)
                .metricFactory(testMetricFactory)
                .build();


        zookeperAttributes = new ZookeeperAttributesDto();
        zookeeperConnectorFactory = Mockito.mock(ZookeeperConnectorFactory.class, withSettings().serializable());

        rulesZookeeperConnector = Mockito.mock(ZookeeperConnector.class, withSettings().serializable());
        when(zookeeperConnectorFactory.createZookeeperConnector(zookeperAttributes))
                .thenReturn(rulesZookeeperConnector);
        when(rulesZookeeperConnector.getData()).thenReturn(testingRules);
        rulesProvider = new ZookeeperRulesProvider(zookeeperConnectorFactory, zookeperAttributes, compiler);

    }

    @Test
    public void testMetadataEngineOk()  {
        ResponseEngine engine = rulesProvider.getEngine();
        Assert.assertEquals(OK, engine.getRulesMetadata().getStatusCode());
        Assert.assertNotNull(engine.getRulesMetadata().getAttributes().getCompiledTime());
        Assert.assertEquals(111L, engine.getRulesMetadata().getAttributes().getRulesVersion().intValue());
        Assert.assertEquals(1L, engine.getRulesMetadata().getAttributes().getNumberOfRules().intValue());
        Assert.assertEquals(testingRules, engine.getRulesMetadata().getAttributes().getJsonRules());
    }

    @Test(expected = java.lang.IllegalStateException.class)
    public void testInvalidRulesInit() throws Exception {
        when(rulesZookeeperConnector.getData()).thenReturn("INVALID");
        rulesProvider = new ZookeeperRulesProvider(zookeeperConnectorFactory, zookeperAttributes, compiler);
    }

    @Test
    public void testEngineEvaluate() throws Exception {
        ResponseAlert alert = new ResponseAlert();
        RespondingResult result = rulesProvider.getEngine().evaluate(alert);
        Assert.assertEquals(OK, result.getStatusCode());
        Assert.assertEquals(MATCH, result.getAttributes().getResult());
    }

}
