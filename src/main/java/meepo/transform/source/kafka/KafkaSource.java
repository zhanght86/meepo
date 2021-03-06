package meepo.transform.source.kafka;

import meepo.transform.channel.DataEvent;
import meepo.transform.channel.RingbufferChannel;
import meepo.transform.config.TaskContext;
import meepo.transform.report.IReportItem;
import meepo.transform.report.SourceReportItem;
import meepo.transform.source.AbstractSource;
import meepo.transform.source.kafka.serialize.IKafkaSerialize;
import meepo.util.Constants;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Properties;

/**
 * Created by peiliping on 17-7-27.
 */
public abstract class KafkaSource extends AbstractSource {

    protected static final Pair<String, byte[]> NULL_MSG = Pair.of(null, null);

    protected TaskContext kafkaContext;

    protected IKafkaSerialize serialize;

    public KafkaSource(String name, int index, int totalNum, TaskContext context, RingbufferChannel rb) {
        super(name, index, totalNum, context, rb);
        this.kafkaContext = new TaskContext(Constants.KAFKA, context.getSubProperties(Constants.KAFKA_));
        String serializeClass = context.getString("serializeClass");
        try {
            TaskContext serializeContext = new TaskContext(Constants.SERIALIZE, context.getSubProperties(Constants.SERIALIZE_));
            Class clazz = Class.forName(serializeClass);
            this.serialize = (IKafkaSerialize) clazz.getConstructor(TaskContext.class).newInstance(serializeContext);
        } catch (Exception e) {
            LOG.error("kafka source serialize :", e);
        }
        this.serialize.initSchema(super.schema);
    }

    protected abstract Pair<String, byte[]> poll(long remain);

    @Override
    public void work() throws Exception {
        Pair<String, byte[]> msg = this.poll(3000);
        if (NULL_MSG == msg)
            return;
        DataEvent de = super.feedOne();
        this.serialize.parse(msg.getRight(), de);
        super.pushOne();
    }

    public abstract Properties toProps();

    @Override
    public IReportItem report() {
        return SourceReportItem.builder().build();
    }
}
