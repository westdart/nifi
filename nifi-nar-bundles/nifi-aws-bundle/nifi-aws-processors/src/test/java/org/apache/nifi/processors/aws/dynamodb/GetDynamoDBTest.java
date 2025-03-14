/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.processors.aws.dynamodb;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.BatchGetItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.TableKeysAndAttributes;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes;
import org.apache.nifi.components.ConfigVerificationResult;
import org.apache.nifi.components.ConfigVerificationResult.Outcome;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.VerifiableProcessor;
import org.apache.nifi.processors.aws.AbstractAWSProcessor;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.nifi.processors.aws.dynamodb.ITAbstractDynamoDBTest.REGION;
import static org.apache.nifi.processors.aws.dynamodb.ITAbstractDynamoDBTest.stringHashStringRangeTableName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GetDynamoDBTest extends AbstractDynamoDBTest {
    protected GetDynamoDB getDynamoDB;
    protected BatchGetItemOutcome outcome;
    protected BatchGetItemResult result = new BatchGetItemResult();
    private HashMap unprocessed;

    @Before
    public void setUp() {
        outcome = new BatchGetItemOutcome(result);
        KeysAndAttributes kaa = new KeysAndAttributes();
        Map<String,AttributeValue> map = new HashMap<>();
        map.put("hashS", new AttributeValue("h1"));
        map.put("rangeS", new AttributeValue("r1"));
        kaa.withKeys(map);
        unprocessed = new HashMap<>();
        unprocessed.put(stringHashStringRangeTableName, kaa);

        result.withUnprocessedKeys(unprocessed);

        Map<String,List<Map<String,AttributeValue>>> responses = new HashMap<>();
        List<Map<String,AttributeValue>> items = new ArrayList<>();
        responses.put("StringHashStringRangeTable", items);
        result.withResponses(responses);

        final DynamoDB mockDynamoDB = new DynamoDB(Regions.AP_NORTHEAST_1) {

            @Override
            public BatchGetItemOutcome batchGetItem(TableKeysAndAttributes... tableKeysAndAttributes) {
                return outcome;
            }

        };

        getDynamoDB = mockDynamoDB(mockDynamoDB);
    }

    @Test
    public void testStringHashStringRangeGetUnprocessed() {
        final TestRunner getRunner = TestRunners.newTestRunner(getDynamoDB);

        getRunner.setProperty(AbstractDynamoDBProcessor.ACCESS_KEY,"abcd");
        getRunner.setProperty(AbstractDynamoDBProcessor.SECRET_KEY, "cdef");
        getRunner.setProperty(AbstractDynamoDBProcessor.REGION, REGION);
        getRunner.setProperty(AbstractDynamoDBProcessor.TABLE, stringHashStringRangeTableName);
        getRunner.setProperty(AbstractDynamoDBProcessor.RANGE_KEY_NAME, "rangeS");
        getRunner.setProperty(AbstractDynamoDBProcessor.HASH_KEY_NAME, "hashS");
        getRunner.setProperty(AbstractDynamoDBProcessor.RANGE_KEY_VALUE, "r1");
        getRunner.setProperty(AbstractDynamoDBProcessor.HASH_KEY_VALUE, "h1");
        getRunner.setProperty(AbstractDynamoDBProcessor.JSON_DOCUMENT, "j1");
        getRunner.enqueue(new byte[] {});

        getRunner.run(1);

        // No actual items returned
        assertVerificationResults(getRunner, 0, 0);

        getRunner.assertAllFlowFilesTransferred(AbstractDynamoDBProcessor.REL_UNPROCESSED, 1);

        List<MockFlowFile> flowFiles = getRunner.getFlowFilesForRelationship(AbstractDynamoDBProcessor.REL_UNPROCESSED);
        for (MockFlowFile flowFile : flowFiles) {
            assertNotNull(flowFile.getAttribute(AbstractDynamoDBProcessor.DYNAMODB_KEY_ERROR_UNPROCESSED));
        }

    }

    @Test
    public void testStringHashStringRangeGetJsonObjectNull() {
        outcome = new BatchGetItemOutcome(result);
        KeysAndAttributes kaa = new KeysAndAttributes();
        Map<String,AttributeValue> map = new HashMap<>();
        map.put("hashS", new AttributeValue("h1"));
        map.put("rangeS", new AttributeValue("r1"));
        kaa.withKeys(map);
        unprocessed = new HashMap<>();
        result.withUnprocessedKeys(unprocessed);

        Map<String,List<Map<String,AttributeValue>>> responses = new HashMap<>();
        List<Map<String,AttributeValue>> items = new ArrayList<>();
        Map<String,AttributeValue> item = new HashMap<String,AttributeValue>();
        item.put("j1",null);
        item.put("hashS", new AttributeValue("h1"));
        item.put("rangeS", new AttributeValue("r1"));
        items.add(item);
        responses.put("StringHashStringRangeTable", items);
        result.withResponses(responses);

        final DynamoDB mockDynamoDB = new DynamoDB(Regions.AP_NORTHEAST_1) {

            @Override
            public BatchGetItemOutcome batchGetItem(TableKeysAndAttributes... tableKeysAndAttributes) {
                return outcome;
            }

        };

        getDynamoDB = mockDynamoDB(mockDynamoDB);
        final TestRunner getRunner = TestRunners.newTestRunner(getDynamoDB);

        getRunner.setProperty(AbstractDynamoDBProcessor.ACCESS_KEY,"abcd");
        getRunner.setProperty(AbstractDynamoDBProcessor.SECRET_KEY, "cdef");
        getRunner.setProperty(AbstractDynamoDBProcessor.REGION, REGION);
        getRunner.setProperty(AbstractDynamoDBProcessor.TABLE, stringHashStringRangeTableName);
        getRunner.setProperty(AbstractDynamoDBProcessor.RANGE_KEY_NAME, "rangeS");
        getRunner.setProperty(AbstractDynamoDBProcessor.HASH_KEY_NAME, "hashS");
        getRunner.setProperty(AbstractDynamoDBProcessor.RANGE_KEY_VALUE, "r1");
        getRunner.setProperty(AbstractDynamoDBProcessor.HASH_KEY_VALUE, "h1");
        getRunner.setProperty(AbstractDynamoDBProcessor.JSON_DOCUMENT, "j1");
        getRunner.enqueue(new byte[] {});

        getRunner.run(1);

        assertVerificationResults(getRunner, 1, 0);

        getRunner.assertAllFlowFilesTransferred(AbstractDynamoDBProcessor.REL_SUCCESS, 1);

        List<MockFlowFile> flowFiles = getRunner.getFlowFilesForRelationship(AbstractDynamoDBProcessor.REL_SUCCESS);
        for (MockFlowFile flowFile : flowFiles) {
            assertNull(flowFile.getContentClaim());
        }

    }

    @Test
    public void testStringHashStringRangeGetJsonObjectValid() throws IOException {
        outcome = new BatchGetItemOutcome(result);
        KeysAndAttributes kaa = new KeysAndAttributes();
        Map<String,AttributeValue> map = new HashMap<>();
        map.put("hashS", new AttributeValue("h1"));
        map.put("rangeS", new AttributeValue("r1"));
        kaa.withKeys(map);
        unprocessed = new HashMap<>();
        result.withUnprocessedKeys(unprocessed);

        Map<String,List<Map<String,AttributeValue>>> responses = new HashMap<>();
        List<Map<String,AttributeValue>> items = new ArrayList<>();
        Map<String,AttributeValue> item = new HashMap<String,AttributeValue>();
        String jsonDocument = "{\"name\": \"john\"}";
        item.put("j1",new AttributeValue(jsonDocument));
        item.put("hashS", new AttributeValue("h1"));
        item.put("rangeS", new AttributeValue("r1"));
        items.add(item);
        responses.put("StringHashStringRangeTable", items);
        result.withResponses(responses);

        final DynamoDB mockDynamoDB = new DynamoDB(Regions.AP_NORTHEAST_1) {

            @Override
            public BatchGetItemOutcome batchGetItem(TableKeysAndAttributes... tableKeysAndAttributes) {
                return outcome;
            }

        };

        getDynamoDB = mockDynamoDB(mockDynamoDB);
        final TestRunner getRunner = TestRunners.newTestRunner(getDynamoDB);

        getRunner.setProperty(AbstractDynamoDBProcessor.ACCESS_KEY,"abcd");
        getRunner.setProperty(AbstractDynamoDBProcessor.SECRET_KEY, "cdef");
        getRunner.setProperty(AbstractDynamoDBProcessor.REGION, REGION);
        getRunner.setProperty(AbstractDynamoDBProcessor.TABLE, stringHashStringRangeTableName);
        getRunner.setProperty(AbstractDynamoDBProcessor.RANGE_KEY_NAME, "rangeS");
        getRunner.setProperty(AbstractDynamoDBProcessor.HASH_KEY_NAME, "hashS");
        getRunner.setProperty(AbstractDynamoDBProcessor.RANGE_KEY_VALUE, "r1");
        getRunner.setProperty(AbstractDynamoDBProcessor.HASH_KEY_VALUE, "h1");
        getRunner.setProperty(AbstractDynamoDBProcessor.JSON_DOCUMENT, "j1");
        getRunner.enqueue(new byte[] {});

        getRunner.run(1);
        assertVerificationResults(getRunner, 1, 1);
        getRunner.assertAllFlowFilesTransferred(AbstractDynamoDBProcessor.REL_SUCCESS, 1);
    }

    @Test
    public void testStringHashStringRangeGetThrowsServiceException() {
        final DynamoDB mockDynamoDB = new DynamoDB(Regions.AP_NORTHEAST_1) {

            @Override
            public BatchGetItemOutcome batchGetItem(TableKeysAndAttributes... tableKeysAndAttributes) {
                throw new AmazonServiceException("serviceException");
            }

        };

        final GetDynamoDB getDynamoDB = mockDynamoDB(mockDynamoDB);

        final TestRunner getRunner = TestRunners.newTestRunner(getDynamoDB);

        getRunner.setProperty(AbstractDynamoDBProcessor.ACCESS_KEY,"abcd");
        getRunner.setProperty(AbstractDynamoDBProcessor.SECRET_KEY, "cdef");
        getRunner.setProperty(AbstractDynamoDBProcessor.REGION, REGION);
        getRunner.setProperty(AbstractDynamoDBProcessor.TABLE, stringHashStringRangeTableName);
        getRunner.setProperty(AbstractDynamoDBProcessor.RANGE_KEY_NAME, "rangeS");
        getRunner.setProperty(AbstractDynamoDBProcessor.HASH_KEY_NAME, "hashS");
        getRunner.setProperty(AbstractDynamoDBProcessor.RANGE_KEY_VALUE, "r1");
        getRunner.setProperty(AbstractDynamoDBProcessor.HASH_KEY_VALUE, "h1");
        getRunner.setProperty(AbstractDynamoDBProcessor.JSON_DOCUMENT, "j1");
        getRunner.enqueue(new byte[] {});

        getRunner.run(1);

        assertVerificationFailure(getRunner);
        getRunner.assertAllFlowFilesTransferred(AbstractDynamoDBProcessor.REL_FAILURE, 1);
        List<MockFlowFile> flowFiles = getRunner.getFlowFilesForRelationship(AbstractDynamoDBProcessor.REL_FAILURE);
        for (MockFlowFile flowFile : flowFiles) {
            assertEquals("serviceException (Service: null; Status Code: 0; Error Code: null; Request ID: null; Proxy: null)",
                    flowFile.getAttribute(AbstractDynamoDBProcessor.DYNAMODB_ERROR_EXCEPTION_MESSAGE));
        }

    }

    @Test
    public void testStringHashStringRangeGetThrowsRuntimeException() {
        final DynamoDB mockDynamoDB = new DynamoDB(Regions.AP_NORTHEAST_1) {

            @Override
            public BatchGetItemOutcome batchGetItem(TableKeysAndAttributes... tableKeysAndAttributes) {
                throw new RuntimeException("runtimeException");
            }

        };

        final GetDynamoDB getDynamoDB = mockDynamoDB(mockDynamoDB);

        final TestRunner getRunner = TestRunners.newTestRunner(getDynamoDB);

        getRunner.setProperty(AbstractDynamoDBProcessor.ACCESS_KEY,"abcd");
        getRunner.setProperty(AbstractDynamoDBProcessor.SECRET_KEY, "cdef");
        getRunner.setProperty(AbstractDynamoDBProcessor.REGION, REGION);
        getRunner.setProperty(AbstractDynamoDBProcessor.TABLE, stringHashStringRangeTableName);
        getRunner.setProperty(AbstractDynamoDBProcessor.RANGE_KEY_NAME, "rangeS");
        getRunner.setProperty(AbstractDynamoDBProcessor.HASH_KEY_NAME, "hashS");
        getRunner.setProperty(AbstractDynamoDBProcessor.RANGE_KEY_VALUE, "r1");
        getRunner.setProperty(AbstractDynamoDBProcessor.HASH_KEY_VALUE, "h1");
        getRunner.setProperty(AbstractDynamoDBProcessor.JSON_DOCUMENT, "j1");
        getRunner.enqueue(new byte[] {});

        getRunner.run(1);

        assertVerificationFailure(getRunner);

        getRunner.assertAllFlowFilesTransferred(AbstractDynamoDBProcessor.REL_FAILURE, 1);
        List<MockFlowFile> flowFiles = getRunner.getFlowFilesForRelationship(AbstractDynamoDBProcessor.REL_FAILURE);
        for (MockFlowFile flowFile : flowFiles) {
            assertEquals("runtimeException", flowFile.getAttribute(AbstractDynamoDBProcessor.DYNAMODB_ERROR_EXCEPTION_MESSAGE));
        }

    }

    @Test
    public void testStringHashStringRangeGetThrowsClientException() {
        final DynamoDB mockDynamoDB = new DynamoDB(Regions.AP_NORTHEAST_1) {

            @Override
            public BatchGetItemOutcome batchGetItem(TableKeysAndAttributes... tableKeysAndAttributes) {
                throw new AmazonClientException("clientException");
            }

        };

        final GetDynamoDB getDynamoDB = mockDynamoDB(mockDynamoDB);

        final TestRunner getRunner = TestRunners.newTestRunner(getDynamoDB);

        getRunner.setProperty(AbstractDynamoDBProcessor.ACCESS_KEY,"abcd");
        getRunner.setProperty(AbstractDynamoDBProcessor.SECRET_KEY, "cdef");
        getRunner.setProperty(AbstractDynamoDBProcessor.REGION, REGION);
        getRunner.setProperty(AbstractDynamoDBProcessor.TABLE, stringHashStringRangeTableName);
        getRunner.setProperty(AbstractDynamoDBProcessor.RANGE_KEY_NAME, "rangeS");
        getRunner.setProperty(AbstractDynamoDBProcessor.HASH_KEY_NAME, "hashS");
        getRunner.setProperty(AbstractDynamoDBProcessor.RANGE_KEY_VALUE, "r1");
        getRunner.setProperty(AbstractDynamoDBProcessor.HASH_KEY_VALUE, "h1");
        getRunner.setProperty(AbstractDynamoDBProcessor.JSON_DOCUMENT, "j1");
        getRunner.enqueue(new byte[] {});

        getRunner.run(1);

        assertVerificationFailure(getRunner);

        getRunner.assertAllFlowFilesTransferred(AbstractDynamoDBProcessor.REL_FAILURE, 1);
        List<MockFlowFile> flowFiles = getRunner.getFlowFilesForRelationship(AbstractDynamoDBProcessor.REL_FAILURE);
        for (MockFlowFile flowFile : flowFiles) {
            assertEquals("clientException", flowFile.getAttribute(AbstractDynamoDBProcessor.DYNAMODB_ERROR_EXCEPTION_MESSAGE));
        }

    }

    @Test
    public void testStringHashStringRangeGetNotFound() {
        result.clearResponsesEntries();
        result.clearUnprocessedKeysEntries();

        final BatchGetItemOutcome notFoundOutcome = new BatchGetItemOutcome(result);
        Map<String,List<Map<String,AttributeValue>>> responses = new HashMap<>();
        List<Map<String,AttributeValue>> items = new ArrayList<>();
        responses.put(stringHashStringRangeTableName, items);
        result.withResponses(responses);

        final DynamoDB notFoundMockDynamoDB = new DynamoDB(Regions.AP_NORTHEAST_1) {
            @Override
            public BatchGetItemOutcome batchGetItem(TableKeysAndAttributes... tableKeysAndAttributes) {
                return notFoundOutcome;
            }
        };

        final GetDynamoDB getDynamoDB = mockDynamoDB(notFoundMockDynamoDB);
        final TestRunner getRunner = TestRunners.newTestRunner(getDynamoDB);

        getRunner.setProperty(AbstractDynamoDBProcessor.ACCESS_KEY,"abcd");
        getRunner.setProperty(AbstractDynamoDBProcessor.SECRET_KEY, "cdef");
        getRunner.setProperty(AbstractDynamoDBProcessor.REGION, REGION);
        getRunner.setProperty(AbstractDynamoDBProcessor.TABLE, stringHashStringRangeTableName);
        getRunner.setProperty(AbstractDynamoDBProcessor.RANGE_KEY_NAME, "rangeS");
        getRunner.setProperty(AbstractDynamoDBProcessor.HASH_KEY_NAME, "hashS");
        getRunner.setProperty(AbstractDynamoDBProcessor.RANGE_KEY_VALUE, "r1");
        getRunner.setProperty(AbstractDynamoDBProcessor.HASH_KEY_VALUE, "h1");
        getRunner.setProperty(AbstractDynamoDBProcessor.JSON_DOCUMENT, "j1");
        getRunner.enqueue(new byte[] {});

        getRunner.run(1);

        assertVerificationResults(getRunner, 0, 0);

        getRunner.assertAllFlowFilesTransferred(GetDynamoDB.REL_NOT_FOUND, 1);

        List<MockFlowFile> flowFiles = getRunner.getFlowFilesForRelationship(AbstractDynamoDBProcessor.REL_UNPROCESSED);
        for (MockFlowFile flowFile : flowFiles) {
            assertNotNull(flowFile.getAttribute(AbstractDynamoDBProcessor.DYNAMODB_KEY_ERROR_NOT_FOUND));
        }

    }

    @Test
    public void testStringHashStringRangeGetOnlyHashFailure() {
        // Inject a mock DynamoDB to create the exception condition
        final DynamoDB mockDynamoDb = Mockito.mock(DynamoDB.class);
        // When writing, mock thrown service exception from AWS
        Mockito.when(mockDynamoDb.batchGetItem(ArgumentMatchers.<TableKeysAndAttributes>any())).thenThrow(getSampleAwsServiceException());

        getDynamoDB = mockDynamoDB(mockDynamoDb);

        final TestRunner getRunner = TestRunners.newTestRunner(getDynamoDB);

        getRunner.setProperty(AbstractDynamoDBProcessor.ACCESS_KEY,"abcd");
        getRunner.setProperty(AbstractDynamoDBProcessor.SECRET_KEY, "cdef");
        getRunner.setProperty(AbstractDynamoDBProcessor.REGION, REGION);
        getRunner.setProperty(AbstractDynamoDBProcessor.TABLE, stringHashStringRangeTableName);
        getRunner.setProperty(AbstractDynamoDBProcessor.HASH_KEY_NAME, "hashS");
        getRunner.setProperty(AbstractDynamoDBProcessor.HASH_KEY_VALUE, "h1");
        getRunner.setProperty(AbstractDynamoDBProcessor.JSON_DOCUMENT, "j1");
        getRunner.enqueue(new byte[] {});

        getRunner.run(1);

        assertVerificationFailure(getRunner);

        getRunner.assertAllFlowFilesTransferred(AbstractDynamoDBProcessor.REL_FAILURE, 1);

        List<MockFlowFile> flowFiles = getRunner.getFlowFilesForRelationship(AbstractDynamoDBProcessor.REL_FAILURE);
        for (MockFlowFile flowFile : flowFiles) {
            ITAbstractDynamoDBTest.validateServiceExceptionAttribute(flowFile);
        }

    }

    @Test
    public void testStringHashStringRangeGetNoHashValueFailure() {
        final TestRunner getRunner = TestRunners.newTestRunner(GetDynamoDB.class);

        getRunner.setProperty(AbstractDynamoDBProcessor.ACCESS_KEY,"abcd");
        getRunner.setProperty(AbstractDynamoDBProcessor.SECRET_KEY, "cdef");
        getRunner.setProperty(AbstractDynamoDBProcessor.REGION, REGION);
        getRunner.setProperty(AbstractDynamoDBProcessor.TABLE, stringHashStringRangeTableName);
        getRunner.setProperty(AbstractDynamoDBProcessor.RANGE_KEY_NAME, "rangeS");
        getRunner.setProperty(AbstractDynamoDBProcessor.HASH_KEY_NAME, "hashS");
        getRunner.setProperty(AbstractDynamoDBProcessor.RANGE_KEY_VALUE, "r1");
        getRunner.setProperty(AbstractDynamoDBProcessor.JSON_DOCUMENT, "j1");
        getRunner.enqueue(new byte[] {});

        getRunner.run(1);

        getRunner.assertAllFlowFilesTransferred(AbstractDynamoDBProcessor.REL_FAILURE, 1);

        List<MockFlowFile> flowFiles = getRunner.getFlowFilesForRelationship(AbstractDynamoDBProcessor.REL_FAILURE);
        for (MockFlowFile flowFile : flowFiles) {
            assertNotNull(flowFile.getAttribute(AbstractDynamoDBProcessor.DYNAMODB_HASH_KEY_VALUE_ERROR));
        }

    }

    @Test
    public void testStringHashStringRangeGetOnlyHashWithRangeValueNoRangeNameFailure() {
        final TestRunner getRunner = TestRunners.newTestRunner(GetDynamoDB.class);

        getRunner.setProperty(AbstractDynamoDBProcessor.ACCESS_KEY,"abcd");
        getRunner.setProperty(AbstractDynamoDBProcessor.SECRET_KEY, "cdef");
        getRunner.setProperty(AbstractDynamoDBProcessor.REGION, REGION);
        getRunner.setProperty(AbstractDynamoDBProcessor.TABLE, stringHashStringRangeTableName);
        getRunner.setProperty(AbstractDynamoDBProcessor.HASH_KEY_NAME, "hashS");
        getRunner.setProperty(AbstractDynamoDBProcessor.HASH_KEY_VALUE, "h1");
        getRunner.setProperty(AbstractDynamoDBProcessor.RANGE_KEY_VALUE, "r1");
        getRunner.setProperty(AbstractDynamoDBProcessor.JSON_DOCUMENT, "j1");
        getRunner.enqueue(new byte[] {});

        getRunner.run(1);

        getRunner.assertAllFlowFilesTransferred(AbstractDynamoDBProcessor.REL_FAILURE, 1);

        List<MockFlowFile> flowFiles = getRunner.getFlowFilesForRelationship(AbstractDynamoDBProcessor.REL_FAILURE);
        for (MockFlowFile flowFile : flowFiles) {
            assertNotNull(flowFile.getAttribute(AbstractDynamoDBProcessor.DYNAMODB_RANGE_KEY_VALUE_ERROR));
        }

    }

    @Test
    public void testStringHashStringRangeGetOnlyHashWithRangeNameNoRangeValueFailure() {
        final TestRunner getRunner = TestRunners.newTestRunner(GetDynamoDB.class);

        getRunner.setProperty(AbstractDynamoDBProcessor.ACCESS_KEY,"abcd");
        getRunner.setProperty(AbstractDynamoDBProcessor.SECRET_KEY, "cdef");
        getRunner.setProperty(AbstractDynamoDBProcessor.REGION, REGION);
        getRunner.setProperty(AbstractDynamoDBProcessor.TABLE, stringHashStringRangeTableName);
        getRunner.setProperty(AbstractDynamoDBProcessor.HASH_KEY_NAME, "hashS");
        getRunner.setProperty(AbstractDynamoDBProcessor.RANGE_KEY_NAME, "rangeS");
        getRunner.setProperty(AbstractDynamoDBProcessor.HASH_KEY_VALUE, "h1");
        getRunner.setProperty(AbstractDynamoDBProcessor.JSON_DOCUMENT, "j1");
        getRunner.enqueue(new byte[] {});

        getRunner.run(1);

        getRunner.assertAllFlowFilesTransferred(AbstractDynamoDBProcessor.REL_FAILURE, 1);

        List<MockFlowFile> flowFiles = getRunner.getFlowFilesForRelationship(AbstractDynamoDBProcessor.REL_FAILURE);
        for (MockFlowFile flowFile : flowFiles) {
            assertNotNull(flowFile.getAttribute(AbstractDynamoDBProcessor.DYNAMODB_RANGE_KEY_VALUE_ERROR));
        }
    }

    // Incorporated test from James W
    @Test
    public void testStringHashStringNoRangeGetUnprocessed() {
        unprocessed.clear();
        KeysAndAttributes kaa = new KeysAndAttributes();
        Map<String,AttributeValue> map = new HashMap<>();
        map.put("hashS", new AttributeValue("h1"));
        kaa.withKeys(map);
        unprocessed.put(stringHashStringRangeTableName, kaa);

        final TestRunner getRunner = TestRunners.newTestRunner(getDynamoDB);

        getRunner.setProperty(AbstractDynamoDBProcessor.ACCESS_KEY,"abcd");
        getRunner.setProperty(AbstractDynamoDBProcessor.SECRET_KEY, "cdef");
        getRunner.setProperty(AbstractDynamoDBProcessor.REGION, REGION);
        getRunner.setProperty(AbstractDynamoDBProcessor.TABLE, stringHashStringRangeTableName);
        getRunner.setProperty(AbstractDynamoDBProcessor.HASH_KEY_NAME, "hashS");
        getRunner.setProperty(AbstractDynamoDBProcessor.HASH_KEY_VALUE, "h1");
        getRunner.setProperty(AbstractDynamoDBProcessor.JSON_DOCUMENT, "j1");
        getRunner.enqueue(new byte[] {});

        getRunner.run(1);

        getRunner.assertAllFlowFilesTransferred(AbstractDynamoDBProcessor.REL_UNPROCESSED, 1);

        List<MockFlowFile> flowFiles = getRunner.getFlowFilesForRelationship(AbstractDynamoDBProcessor.REL_UNPROCESSED);
        for (MockFlowFile flowFile : flowFiles) {
            assertNotNull(flowFile.getAttribute(AbstractDynamoDBProcessor.DYNAMODB_KEY_ERROR_UNPROCESSED));
        }
    }

    private GetDynamoDB mockDynamoDB(final DynamoDB mockDynamoDB) {
        return new GetDynamoDB() {
            @Override
            protected DynamoDB getDynamoDB() {
                return mockDynamoDB;
            }
            @Override
            protected DynamoDB getDynamoDB(final AmazonDynamoDBClient client) {
                return mockDynamoDB;
            }

            @Override
            protected AbstractAWSProcessor<AmazonDynamoDBClient>.AWSConfiguration getConfiguration(final ProcessContext context) {
                final AmazonDynamoDBClient client = Mockito.mock(AmazonDynamoDBClient.class);
                return new AWSConfiguration(client, region);
            }
        };
    }

    private void assertVerificationFailure(final TestRunner runner) {
        final List<ConfigVerificationResult> results = ((VerifiableProcessor) runner.getProcessor())
                .verify(runner.getProcessContext(), runner.getLogger(), Collections.emptyMap());
        assertEquals(3, results.size());
        assertEquals(Outcome.SUCCESSFUL, results.get(0).getOutcome());
        assertEquals(Outcome.SUCCESSFUL, results.get(1).getOutcome());
        assertEquals(Outcome.FAILED, results.get(2).getOutcome());
    }

    private void assertVerificationResults(final TestRunner runner, final int expectedTotalCount, final int expectedJsonDocumentCount) {
        final List<ConfigVerificationResult> results = ((VerifiableProcessor) runner.getProcessor())
                .verify(runner.getProcessContext(), runner.getLogger(), Collections.emptyMap());
        assertEquals(3, results.size());
        results.forEach(result -> assertEquals(Outcome.SUCCESSFUL, result.getOutcome()));
        assertTrue(results.get(2).getExplanation().contains("retrieved " + expectedTotalCount + " items"));
        assertTrue(results.get(2).getExplanation().contains(expectedJsonDocumentCount + " JSON"));
    }
}
