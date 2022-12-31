package chen.yyds.py.code;

import com.intellij.lang.Language;

public class PythonLanguage extends Language {
    public static final PythonLanguage INSTANCE = new PythonLanguage();
    protected PythonLanguage() {
        super("Py");
    }
}
