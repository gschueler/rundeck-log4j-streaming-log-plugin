Install in $RDECK_BASE/libext/

Enable by adding this to `rundeck-config.properties`:

    rundeck.execution.logs.streamingWriterPlugins=log4jStreamingLogWriterPlugin

Configure in `framework.properties` to set the logger name:

    framework.plugin.StreamingLogWriter.log4jStreamingLogWriterPlugin.loggername=org.rundeck.log.event.stream

If necessary modify these config values:

* `loggername` : name of log4j logger to use
* `jobsOnly`: 'true' means only log events for jobs, not adhoc executions
* `includeLogs`: 'true' means include output log messages from the execution
* `logNewNode`: 'true' means log when a new node is seen for the execution

Configure log4j.properties:

    log4j.logger.org.rundeck.log.event.stream=info,executionevents,stdout
    log4j.additivity.org.rundeck.log.event.stream=false

    log4j.appender.executionevents=org.apache.log4j.DailyRollingFileAppender
    log4j.appender.executionevents.file=/var/log/rundeck/rundeck.executionevents.log
    log4j.appender.executionevents.append=true
    log4j.appender.executionevents.layout=org.apache.log4j.PatternLayout
    log4j.appender.executionevents.layout.ConversionPattern=%d{ISO8601} execution: %X{execid} (%X{event}, %X{nodename}) %X{group}/%X{name} [%X{id}] - %m%n

Context properties available in log4j MDC:

* execid - execution ID
* event - one of 'start','end','node','log'
* nodename - name of new node seen for a 'node' event
* group - job group
* name - job name
* id - job uuid
* url - execution URL
* serverUrl - server URL


