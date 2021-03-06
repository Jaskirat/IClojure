package com.offbytwo.iclojure.completion;

import clojure.lang.LazySeq;
import clojure.lang.RT;
import clojure.lang.Var;
import jline.console.completer.Completer;
import jline.internal.Log;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import static java.util.Arrays.asList;

public class ClojureCompleter implements Completer {
    private final Var eval;
    private final Var completions;

    public ClojureCompleter() throws ClassNotFoundException, IOException {
        RT.load("complete");
        this.eval = RT.var("clojure.core", "eval");
        this.completions = RT.var("complete", "completions");
    }


    public int complete(String buffer, int cursor, List<CharSequence> candidates) {
        String symbolToComplete;
        int matchStart;

        if (cursor <= buffer.length()) {
            buffer = buffer.substring(0, cursor);
        }

        if (buffer.startsWith("?")) {
            matchStart = 1;
            if (buffer.startsWith("??")) {
                matchStart = 2;
            }

            symbolToComplete = buffer.substring(matchStart);
        } else if (buffer.startsWith("%d")) {
            matchStart = 3;
            symbolToComplete = buffer.substring(matchStart);
        } else {
            matchStart = 0;

            if (buffer.startsWith(" ")) {
                String trimmed = buffer.trim();
                matchStart += buffer.indexOf(trimmed.charAt(0));
                buffer = buffer.substring(matchStart);
            }

            if (buffer.startsWith("(. ")) {
                if (buffer.lastIndexOf(' ') > 2) {
                    // there is a previous form
                    String prefix;
                    if (buffer.lastIndexOf(' ') == cursor - 1) {
                        prefix = "";
                        matchStart = cursor;
                    } else {
                        prefix = buffer.substring(buffer.lastIndexOf(' ') + 1);
                        matchStart += buffer.lastIndexOf(' ') + 1;
                    }

                    String form = buffer.replaceFirst("\\(\\. ", "").trim();
                    Object output = eval.invoke(RT.readString(form));
                    for (Method m : output.getClass().getMethods()) {
                        if (m.getName().startsWith(prefix)) {
                            candidates.add(m.getName());
                        }
                    }
                    return matchStart;
                } else {
                    // this is the first form we are trying to complete
                    matchStart += 3;
                    symbolToComplete = buffer.substring(3);
                }
            } else if (buffer.startsWith("(")) {
                symbolToComplete = buffer.replaceFirst("\\(", "");
                matchStart += 1;
            } else {
                symbolToComplete = buffer;
            }
        }

        try {
            LazySeq results = (LazySeq) completions.invoke(symbolToComplete);
            for (Object result : asList(results.toArray())) {
                candidates.add((String) result);
            }
            return matchStart;
        } catch (RuntimeException re) {
            return 0;
        }

    }
}
