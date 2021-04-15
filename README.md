# mqsampler

MQ JMeter Extension.

A [JMeter](http://jmeter.apache.org/) Plugin to put and get message on [IBM MQ](https://www.ibm.com/products/mq) Queue, also publish message on Topic. It connect to MQ Server through server channel using ip address, port number, userID and password (if the channel has CHLAUTH rules).

## Install

Build the extension:

    mvn package

Install the extension `mqmeter-x.y.z.jar` into 

    `$JMETER_HOME/lib/ext`.

Also you can install it through [JMeter Plugins](http://jmeter.apache.org/), search "IBM MQ Support".

![Screenshot](https://github.com/JoseLuisSR/mqmeter/blob/develop/doc/img/jmete-plugins-mqmeter.png?raw=true)

## Usage

After installing `mqmeter`, you can choose two kind of Java Sampler, these are:

### MQClientSampler

Use it to put and get message (optional) on MQ queue. On JMeter add a Java Request Sampler and select the `MQClientSampler` class name. The following parameter are necessary.
