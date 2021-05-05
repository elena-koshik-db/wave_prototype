package org.apache.beam.examples;

import org.apache.beam.examples.common.WriteOneFilePerWindow;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.StreamingOptions;
import org.apache.beam.sdk.options.Validation;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.joda.time.Duration;

import java.io.IOException;


public class StreamingWordCount {
    static final int WINDOW_SIZE = 5; // Default window duration in minutes
    static final int NUM_SHARDS = 1;// Default number of shards to produce per window
    static final String BUCKET_PATH = "gs://pub_sub_example/output";

    public interface Options extends StreamingOptions {
        @Description("Input PubSub topic of the form 'projects/<PROJECT>/topics/<TOPIC>'")
        ValueProvider<String> getInputTopic();

        void setInputTopic(ValueProvider<String> topic);

        /*@Description("Path of the dir to write to")
        @Validation.Required
        ValueProvider<String> getOutput();

        void setOutput(ValueProvider<String> value);*/
    }

    static void runStreamingWordCount(Options options) throws IOException {
        options.setStreaming(true);

        Pipeline pipeline = Pipeline.create(options);

        pipeline
                .apply("Read PubSub messages", PubsubIO.readStrings().fromTopic(options.getInputTopic()))
                .apply(Window.into(FixedWindows.of(Duration.standardMinutes(WINDOW_SIZE))))
                .apply(new WordCount.CountWords())
                .apply(MapElements.via(new WordCount.FormatAsTextFn()))
                .apply("Write Files to GCS", new WriteOneFilePerWindow(BUCKET_PATH, NUM_SHARDS));

        PipelineResult result = pipeline.run();
        try {
            result.waitUntilFinish();
        } catch (Exception exc) {
            result.cancel();
        }
    }

    public static void main(String[] args) throws IOException {
        Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);

        runStreamingWordCount(options);
    }
}
