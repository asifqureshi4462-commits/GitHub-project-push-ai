package com.asifqureshi.gitforge.model;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.List;

public class AIModels {

    // Request for Gemini API generateContent endpoint
    public static class GeminiRequest {
        @SerializedName("contents")
        private List<Content> contents;

        public GeminiRequest(String promptText) {
            this.contents = Collections.singletonList(new Content(promptText));
        }
    }

    public static class Content {
        @SerializedName("parts")
        private List<Part> parts;

        public Content(String text) {
            this.parts = Collections.singletonList(new Part(text));
        }
    }

    public static class Part {
        @SerializedName("text")
        private String text;

        public Part(String text) {
            this.text = text;
        }

        public String getText() { return text; }
    }

    // Response from Gemini API generateContent
    public static class GeminiResponse {
        @SerializedName("candidates")
        private List<Candidate> candidates;

        public List<Candidate> getCandidates() { return candidates; }

        public String getFirstCandidateText() {
            if (candidates != null && !candidates.isEmpty()) {
                Candidate first = candidates.get(0);
                if (first.content != null && first.content.parts != null && !first.content.parts.isEmpty()) {
                    return first.content.parts.get(0).getText();
                }
            }
            return "";
        }
    }

    public static class Candidate {
        @SerializedName("content")
        private Content content;
    }
}
