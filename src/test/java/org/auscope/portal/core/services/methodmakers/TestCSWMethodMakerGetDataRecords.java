package org.auscope.portal.core.services.methodmakers;

import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.auscope.portal.core.services.methodmakers.CSWMethodMakerGetDataRecords.ResultType;
import org.auscope.portal.core.services.methodmakers.filter.csw.CSWGetDataRecordsFilter;
import org.auscope.portal.core.test.PortalTestClass;
import org.auscope.portal.core.util.DOMUtil;
import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for CSWMethodMakerGetDataRecords
 * @author Josh Vote
 *
 */
public class TestCSWMethodMakerGetDataRecords extends PortalTestClass  {

    private static final String uri = "http://test.com";

    private CSWMethodMakerGetDataRecords methodMaker;
    private CSWGetDataRecordsFilter mockFilter;

    /**
     * Setup each unit test
     */
    @Before
    public void init() {
        mockFilter = context.mock(CSWGetDataRecordsFilter.class);
        methodMaker = new CSWMethodMakerGetDataRecords();
    }

    /**
     * Simple test to ensure that makeMethod includes all appropriate fields
     */
    @Test
    public void testMakeMethodFilter() throws Exception {
        final int maxRecords = 1234;
        final String filterStr = "<filter/>";

        context.checking(new Expectations() {{
            oneOf(mockFilter).getFilterStringAllRecords();will(returnValue(filterStr));
        }});

        HttpMethodBase method = methodMaker.makeMethod(uri, mockFilter, ResultType.Results, maxRecords);
        Assert.assertNotNull(method);
        Assert.assertTrue(method instanceof PostMethod); //we want this to be sent via post in case we get a large filter

        RequestEntity entity = ((PostMethod) method).getRequestEntity();
        Assert.assertTrue(entity instanceof StringRequestEntity);
        String postBody = ((StringRequestEntity) entity).getContent();

        Assert.assertTrue(postBody.contains(String.format("maxRecords=\"%1$s\"", maxRecords)));
        Assert.assertTrue(postBody.contains(String.format("resultType=\"results\"")));
        Assert.assertTrue(postBody.contains(filterStr));

        Assert.assertNotNull(DOMUtil.buildDomFromString(postBody));//this should NOT throw an exception
    }

    /**
     * Simple test to ensure that makeMethod includes all appropriate fields
     */
    @Test
    public void testMakeMethodNoFilter() throws Exception {
        final int maxRecords = 14;

        context.checking(new Expectations() {{

        }});

        HttpMethodBase method = methodMaker.makeMethod(uri, null, ResultType.Hits, maxRecords);
        Assert.assertNotNull(method);
        Assert.assertTrue(method instanceof PostMethod); //we want this to be sent via post in case we get a large filter

        RequestEntity entity = ((PostMethod) method).getRequestEntity();
        Assert.assertTrue(entity instanceof StringRequestEntity);
        String postBody = ((StringRequestEntity) entity).getContent();

        Assert.assertTrue(postBody.contains(String.format("maxRecords=\"%1$s\"", maxRecords)));
        Assert.assertTrue(postBody.contains(String.format("resultType=\"hits\"")));
        Assert.assertFalse(postBody.contains("csw:Constraint"));

        Assert.assertNotNull(DOMUtil.buildDomFromString(postBody));//this should NOT throw an exception
    }
}
