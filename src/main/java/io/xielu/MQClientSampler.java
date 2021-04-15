package io.xielu;

import com.aliyun.mq.http.MQClient;
import com.aliyun.mq.http.MQProducer;
import com.aliyun.mq.http.model.TopicMessage;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

/**
 * This is a jmeter plugin for RocketMQ Test
 */

public class MQClientSampler extends AbstractJavaSamplerClient {

    private static final Logger log = LoggingManager.getLoggerForClass();

    /**
     * Parameter for setting Instance ID
     */
    private static final String PARAMETER_MQ_INSTANCE_ID = "mq_instance_id";

    /**
     * Parameter for setting MQ Topic
     */
    private static final String PARAMETER_MQ_TOPIC = "mq_topic";

    /**
     * Parameter for setting Access Key
     */
    private static final String PARAMETER_MQ_ACCESS_KEY = "mq_access_key";

    /**
     * Parameter for setting Secret Key
     */
    private static final String PARAMETER_MQ_SECRET_KEY = "mq_secret_key";

    /**
     * Parameter for setting MQ Channel, it should be server connection channel.
     */
    private static final String PARAMETER_MQ_CHANNEL = "mq_channel";

    /**
     * Parameter for setting MQ Message.
     */
    private static final String PARAMETER_MQ_MESSAGE = "mq_message";

    /**
     * Parameter for setting MQ Message Label
     */
    private static final String PARAMETER_MQ_MESSAGE_LABEL = "mq_message_label";

    /**
     * Parameter for HTTP Endpoint
     */
    private static final String PARAMETER_MQ_HTTP_ENDPOINT = "mq_http_endpoint";

    /**
     * Parameter for Time Interval,miliseconds
     */
    private static final String PARAMETER_MQ_INTERVAL = "mq_interval";

    /**
     * Parameter for messages count to send
     */
    private static final String PARAMETER_MQ_MSG_CNT = "mq_msg_cnt";

    /**
     * Parameter for message key
     */
    private static final String PARAMETER_MQ_MSG_KEY = "mq_msg_key";

    /**
     * MQ Client
     */
    private MQClient mqClient;

    /**
     * Encoding
     */
    private static final String ENCODING = "UTF-8";

    /**
     * Initial values for test parameter. They are show in Java Request test sampler.
     * @return Arguments to set as default.
     */
    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameter = new Arguments();
        defaultParameter.addArgument(PARAMETER_MQ_INSTANCE_ID, "${MQ_INSTANCE_ID}");
        defaultParameter.addArgument(PARAMETER_MQ_TOPIC, "${MQ_TOPIC}");
        defaultParameter.addArgument(PARAMETER_MQ_ACCESS_KEY, "${MQ_ACCESS_KEY}");
        defaultParameter.addArgument(PARAMETER_MQ_SECRET_KEY, "${MQ_SECRET_KEY}");
        defaultParameter.addArgument(PARAMETER_MQ_MESSAGE_LABEL, "${MQ_MESSAGE_LABEL}");
        defaultParameter.addArgument(PARAMETER_MQ_CHANNEL, "${MQ_CHANNEL}");
        defaultParameter.addArgument(PARAMETER_MQ_HTTP_ENDPOINT,"${MQ_HTTP_ENDPOINT}");
        defaultParameter.addArgument(PARAMETER_MQ_MESSAGE, "${MQ_MESSAGE}");
        defaultParameter.addArgument(PARAMETER_MQ_INTERVAL,"${MQ_INTERVAL}");
        defaultParameter.addArgument(PARAMETER_MQ_MSG_CNT,"$MQ_MSG_CNT");
        defaultParameter.addArgument(PARAMETER_MQ_MSG_KEY,"$MQ_MSG_KEY");
        return defaultParameter;
    }

    /**
     * Read the test parameter and initialize your test client.
     * @param context to get the arguments values on Java Sampler.
     */
    @Override
    public void setupTest(JavaSamplerContext context) {
        String mqAccessKey = context.getParameter(PARAMETER_MQ_ACCESS_KEY);
        String mqSecretKey = context.getParameter(PARAMETER_MQ_SECRET_KEY);
        String mqHttpEndpoint = context.getParameter(PARAMETER_MQ_HTTP_ENDPOINT);

        log.info("MQ Client is setup as below: HttpEndpoint: "+ mqHttpEndpoint + " AccessKey: " + mqAccessKey + " Secret Key: " + mqSecretKey);

        mqClient = new MQClient(
                    mqHttpEndpoint,
                    mqAccessKey,
                    mqSecretKey
            );
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        String mqTopic = context.getParameter(PARAMETER_MQ_TOPIC);
        String mqMessage = context.getParameter(PARAMETER_MQ_MESSAGE);
        String mqMessageLabel = context.getParameter(PARAMETER_MQ_MESSAGE_LABEL);
        String mqInterval = context.getParameter(PARAMETER_MQ_INTERVAL);
        String mqInstanceID = context.getParameter(PARAMETER_MQ_INSTANCE_ID);
        String mqMsgCnt = context.getParameter(PARAMETER_MQ_MSG_CNT);
        String mqMsgKey = context.getParameter(PARAMETER_MQ_MSG_KEY);
        Integer mqIntervalNum = 0;
        Integer mqMsgCntNum = 1;

        String response = null;
        try{
            mqIntervalNum = Integer.parseInt(mqInterval);
            mqMsgCntNum = Integer.parseInt(mqMsgCnt);
        } catch (NumberFormatException e){
            log.error("NumberFormatException: The interval is set as 0/nThe messages count is set as 1");
        }

        SampleResult result = newSampleResult();

        MQProducer producer;
        if (mqInstanceID != null && mqInstanceID != "") {
            producer = mqClient.getProducer(mqInstanceID, mqTopic);
        } else {
            producer = mqClient.getProducer(mqTopic);
        }

        sampleResultStart(result, mqMessage);

        try{
            TopicMessage pubMsg;
            for (int i = 0;i < mqMsgCntNum;i++){
                pubMsg = new TopicMessage(
                        mqMessage.getBytes(),
                        mqMessageLabel
                );
                pubMsg.getProperties().put(mqMessageLabel, String.valueOf(i));

                pubMsg.setMessageKey(mqMsgKey);

                if(mqIntervalNum > 0){
                    pubMsg.setStartDeliverTime(System.currentTimeMillis() + mqIntervalNum);
                }

                TopicMessage pubResultMsg = producer.publishMessage(pubMsg);
                log.info(new Date() + " Send mq message success. Topic is:" + mqTopic + ", msgId is: " + pubResultMsg.getMessageId()
                        + ", bodyMD5 is: " + pubResultMsg.getMessageBodyMD5());
                response = pubResultMsg.getMessageId();
                sampleResultSuccess(result, response);
            }
        } catch (Exception e){
            sampleResultFail(result, "500", e);
            log.info("runTest " + e.getMessage());
        } finally {
            try{
                log.info("Close the topic");
                mqClient.close();
                log.info("Done!");
            }catch (Exception e){
                sampleResultFail(result, "500", e);
                log.info("runTest " + e.getMessage());
            }
        }


        return result;
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {
        try{
            mqClient.close();
            log.info("Test complete");
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * Start the sample request and set the <code>samplerData</code> to the
     * requestData.
     *
     * @param result
     *          the sample result to update
     * @param data
     *          the request to set as <code>samplerData</code>
     */
    private void sampleResultStart(SampleResult result, String data){
        result.setSamplerData(data);
        result.sampleStart();
    }

    /**
     * Set the sample result as <code>sampleEnd()</code>,
     * <code>setSuccessful(true)</code>, <code>setResponseCode("OK")</code> and if
     * the response is not <code>null</code> then
     * <code>setResponseData(response.toString(), ENCODING)</code> otherwise it is
     * marked as not requiring a response.
     *
     * @param result
     *          sample result to change
     * @param response
     *          the successful result message, may be null.
     */
    private void sampleResultSuccess(SampleResult result, String response){
        result.sampleEnd();
        result.setSuccessful(true);
        result.setResponseCodeOK();
        if(response != null)
            result.setResponseData(response, ENCODING);
        else
            result.setResponseData("No response required", ENCODING);
    }

    /**
     *
     * @return SampleResult, captures data such as whether the test was successful,
     * the response code and message, any request or response data, and the test start/end times
     */
    private SampleResult newSampleResult(){
        SampleResult result = new SampleResult();
        result.setDataEncoding(ENCODING);
        result.setDataType(SampleResult.TEXT);
        return result;
    }

    /**
     * Mark the sample result as <code>sampleEnd</code>,
     * <code>setSuccessful(false)</code> and the <code>setResponseCode</code> to
     * reason.
     *
     * @param result
     *          the sample result to change
     * @param reason
     *          the failure reason
     */
    private void sampleResultFail(SampleResult result, String reason, Exception exception){
        result.sampleEnd();
        result.setSuccessful(false);
        result.setResponseCode(reason);
        String responseMessage;

        responseMessage = "Exception: " + exception.getMessage();
        result.setResponseMessage(responseMessage);
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        result.setResponseData(stringWriter.toString(), ENCODING);
    }
}
