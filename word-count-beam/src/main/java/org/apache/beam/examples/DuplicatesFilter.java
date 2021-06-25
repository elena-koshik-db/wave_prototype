package org.apache.beam.examples;

import org.apache.beam.examples.common.WriteOneFilePerWindow;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.StreamingOptions;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Duration;

public class DuplicatesFilter {
    private static long WINDOW_SIZE_MIN = 10;
    static final String BUCKET_PATH = "gs://pub_sub_example/output/test";

    public interface Options extends StreamingOptions {
        @Description("Input PubSub topic of the form 'projects/<PROJECT>/topics/<TOPIC>'")
        String getInputTopic();

        void setInputTopic(String topic);
    }

    static void runDuplicatesFilter(Options options) {
        Pipeline pipeline = Pipeline.create(options);

        pipeline
                .apply("Read PubSub messages", PubsubIO.readStrings().fromTopic(options.getInputTopic()))
                .apply(new Filter(WINDOW_SIZE_MIN))
                .apply(ToString.elements())
                .apply("Write Files to GCS", new WriteOneFilePerWindow(BUCKET_PATH, null));

        pipeline.run();
    }

    public static class Filter extends PTransform<PCollection<String>, PCollection<Element>> {
        private final long windowSizeMin;

        public Filter(long windowSizeMin) {
            this.windowSizeMin = windowSizeMin;
        }

        @Override
        public PCollection<Element> expand(PCollection<String> input) {
            return input
                    .apply(MapElements.via(new SimpleFunction<String, Element>() {
                        @Override
                        public Element apply(String input) {
                            return new Element(input);
                        }
                    }))
                    .apply(Window.into(FixedWindows.of(Duration.standardMinutes(windowSizeMin))))
                    .apply(Distinct.withRepresentativeValueFn(s -> s.id1));
        }
    }

    public static class Element {
        final String id1;

        public Element(String id1) {
            this.id1 = id1;
        }

        @Override
        public String toString() {
            return id1;
        }
    }

    public static void main(String[] args) {
        Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);

        runDuplicatesFilter(options);
    }
}
