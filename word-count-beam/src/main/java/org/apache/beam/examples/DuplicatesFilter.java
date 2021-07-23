package org.apache.beam.examples;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.BooleanCoder;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.StreamingOptions;
import org.apache.beam.sdk.state.StateSpec;
import org.apache.beam.sdk.state.StateSpecs;
import org.apache.beam.sdk.state.ValueState;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.values.KV;


public class DuplicatesFilter {
    private static long WINDOW_SIZE_MIN = 60;
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
                .apply(MapElements.via(new SimpleFunction<String, KV<String, Void>>() {
                    @Override
                    public KV<String, Void> apply(String input) {
                        return KV.of(input, (Void) null);
                    }
                }))
                .apply(ParDo.of(new DeduplicateFn()))
                .apply(MapElements.via(new SimpleFunction<KV<String, Void>, String>() {
                    @Override
                    public String apply(KV<String, Void> input) {
                        return input.getKey();
                    }
                }))
//                .apply(Deduplicate.<String>values().withDuration(Duration.standardMinutes(WINDOW_SIZE_MIN)))
                .apply(ToString.elements())
                .apply(PubsubIO.writeStrings().to("projects/sandbox-307310/topics/filter-out"));

        pipeline.run();
    }

    private static class DeduplicateFn extends DoFn<KV<String, Void>, KV<String, Void>> {
        private static final String EXPIRY_TIMER = "expiryTimer";
        private static final String SEEN_STATE = "seen";

//        @TimerId(EXPIRY_TIMER)
//        private final TimerSpec expiryTimerSpec;

        @StateId(SEEN_STATE)
        private final StateSpec<ValueState<Boolean>> seenState = StateSpecs.value(BooleanCoder.of());

        @ProcessElement
        public void processElement(
                @Element KV<String, Void> element,
                OutputReceiver<KV<String, Void>> receiver,
                @StateId(SEEN_STATE) ValueState<Boolean> seenState) {
//                @TimerId(EXPIRY_TIMER) Timer expiryTimer) {
            Boolean seen = seenState.read();
            // Seen state is either set or not set so if it has been set then it must be true.
            if (seen == null) {
                seenState.write(true);
                receiver.output(element);
            } else {
                receiver.output(KV.of(element.getKey() + "~~~", element.getValue()));
            }
        }
    }

    public static void main(String[] args) {
        Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);

        runDuplicatesFilter(options);
    }
}
