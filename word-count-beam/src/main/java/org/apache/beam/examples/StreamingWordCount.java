package org.apache.beam.examples;

import org.apache.beam.examples.common.ExampleBigQueryTableOptions;
import org.apache.beam.examples.common.ExampleOptions;
import org.apache.beam.examples.common.WriteOneFilePerWindow;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.StreamingOptions;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.joda.time.Duration;


public class StreamingWordCount {
    static final int WINDOW_SIZE = 10; // Default window duration in minutes
    static final int NUM_SHARDS = 1;// Default number of shards to produce per window

    public interface Options
            extends WordCount.WordCountOptions, ExampleOptions, ExampleBigQueryTableOptions, StreamingOptions {
        @Description("Fixed window duration, in minutes")
        @Default.Integer(WINDOW_SIZE)
        Integer getWindowSize();

        void setWindowSize(Integer value);

        @Description("Fixed number of shards to produce per window")
        @Default.Integer(NUM_SHARDS)
        Integer getNumShards();

        void setNumShards(Integer numShards);

        @Description("Input PubSub topic of the form 'projects/<PROJECT>/topics/<TOPIC>'")
        String getInputTopic();

        void setInputTopic(String topic);
    }

    static void runStreamingWordCount(Options options) {
        options.setStreaming(true);

        Pipeline pipeline = Pipeline.create(options);

        pipeline
                .apply("Read PubSub messages", PubsubIO.readStrings().fromTopic(options.getInputTopic()))
                .apply(Window.into(FixedWindows.of(Duration.standardMinutes(options.getWindowSize()))))
                .apply(new WordCount.CountWords())
                .apply(MapElements.via(new WordCount.FormatAsTextFn()))
                .apply("Write Files to GCS", new WriteOneFilePerWindow(options.getOutput(), options.getNumShards()));
    }

    public static void main(String[] args) {
        Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);

        runStreamingWordCount(options);
    }
}
