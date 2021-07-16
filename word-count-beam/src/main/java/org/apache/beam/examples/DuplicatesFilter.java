package org.apache.beam.examples;

import org.apache.beam.examples.common.WriteOneFilePerWindow;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.StreamingOptions;
import org.apache.beam.sdk.transforms.Deduplicate;
import org.apache.beam.sdk.transforms.ToString;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.joda.time.Duration;


public class DuplicatesFilter {
    private static long WINDOW_SIZE_MIN = 20;
    static final String BUCKET_PATH = "gs://pub_sub_example/output/test";

    public interface Options extends StreamingOptions {
        @Description("Input PubSub topic of the form 'projects/<PROJECT>/topics/<TOPIC>'")
        String getInputTopic();

        void setInputTopic(String topic);
    }

    static void runDuplicatesFilter(Options options) {
        options.setStreaming(true);

        Pipeline pipeline = Pipeline.create(options);

        pipeline
                .apply("Read PubSub messages", PubsubIO.readStrings().fromTopic(options.getInputTopic()))
                .apply(Deduplicate.<String>values().withDuration(Duration.standardMinutes(WINDOW_SIZE_MIN)))
                .apply(ToString.elements())
                .apply(PubsubIO.writeStrings().to("projects/sandbox-307310/topics/filter-out"));

        pipeline.run();
    }

    public static void main(String[] args) {
        Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);

        runDuplicatesFilter(options);
    }
}
