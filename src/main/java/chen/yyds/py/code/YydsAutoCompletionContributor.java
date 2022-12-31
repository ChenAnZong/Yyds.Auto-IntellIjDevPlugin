package chen.yyds.py.code;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

public class YydsAutoCompletionContributor extends CompletionContributor {
    public YydsAutoCompletionContributor() {
        extend(CompletionType.SMART,
                PlatformPatterns.psiElement().withLanguage(PythonLanguage.INSTANCE),
                new CompletionProvider<>() {
                    public void addCompletions(@NotNull CompletionParameters parameters,
                                               @NotNull ProcessingContext context,
                                               @NotNull CompletionResultSet resultSet) {
                        resultSet.addElement(LookupElementBuilder.create("Yyds"));
                    }
                }
        );
    }
}
