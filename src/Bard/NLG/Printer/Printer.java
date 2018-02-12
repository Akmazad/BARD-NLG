package Bard.NLG.Printer;

import simplenlg.features.Feature;
import simplenlg.features.NumberAgreement;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.VPPhraseSpec;
import simplenlg.realiser.english.Realiser;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static Bard.NLG.Tools.DEBUG;
import static Bard.NLG.Printer.Printer.SentenceControl.NO;
import static Bard.NLG.Tools.shouldNotHappen;

public class Printer {


    // --- --- --- Fields

    private Deque<Comp> compStack;
    private StringBuilder sb;

    // text control
    private boolean inSentence;
    private boolean forceSpace; // Special management of space for closing tags like 'span'

    // SimpleNLG tools
    public static final Lexicon lexicon = Lexicon.getDefaultLexicon();
    public static final NLGFactory nlgFactory = new NLGFactory(lexicon);
    public static final Realiser realiser = new Realiser(lexicon);


    // --- --- --- Constructor

    public Printer() {
        this.compStack = new ArrayDeque<>();
        this.sb = new StringBuilder();
        this.inSentence = false;
        this.forceSpace = false;
        // Initial component
        this.compStack.push(new Comp());
        // CSS
        this.sb.append(PrinterHelper.cssString);
    }


    // --- --- --- Main methods

    public String realize() {
        if (DEBUG && compStack.size() != 1) {
            throw new RuntimeException("CompStack is suppose to be of size 1, found " + compStack.size());
        }
        compStack.peek().render();
        return sb.toString();
    }


    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    // HTML Main Methods
    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

    public Printer openTag(String tag) {
        this.compStack.push(this.new Comp(tag));
        return this;
    }

    public Printer closeTag(String tag) {
        Comp top = this.compStack.pop();
        if (top.tag != null && top.tag.equals(tag)) {
            this.compStack.peek().addChild(top);
        } else {
            throw new RuntimeException("Can not close tag `" + tag + "`: top of the stack is `" + top.tag + "`");
        }
        return this;
    }

    // Embed simple text in a tag.
    public Printer embedInTag(String tag, String content) {
        openTag(tag);
        compStack.peek().setTxtContent(content);
        closeTag(tag);
        return this;
    }

    // Apply the updateComp function on the top of the stack
    public Printer onTop(Consumer<Comp> updateComp) {
        updateComp.accept(getTop());
        return this;
    }

    // Apply the updateComp function on the last children of the Comp of the top of the stack = last closeTag
    public Printer onLast(Consumer<Comp> updateComp) {
        updateComp.accept(getLast());
        return this;
    }


    // --- --- --- Component handling

    public Comp pushComp() {
        Comp c = this.new Comp();
        this.compStack.push(c);
        return c;
    }

    public Comp getTop() {
        return this.compStack.peek();
    }

    public Comp addComp() {
        Comp c = this.new Comp();
        getTop().addChild(c);
        return c;
    }

    public Comp getLast() {
        List<Comp> children = this.compStack.peek().children;
        // Skip new line node, are they are only their for the source layout.
        for (int i = children.size() - 1; i >= 0; --i) {
            Comp c = children.get(i);
            if (!c.doNL) {
                return c;
            }
        }
        return null;
    }

    // Embed a component in a tag
    public Printer embedInTag(String tag, Comp comp) {
        openTag(tag);
        compStack.peek().addChild(comp);
        closeTag(tag);
        return this;
    }


    // --- --- --- Misc Printer

    public Printer apply(Function<Printer, Printer> fun){
        return fun.apply(this);
    }

    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    // TEXT methods
    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

    public Printer addText(String text) {
        getTop().addChild(this.new Comp().setTxtContent(text));
        return this;
    }

    public Printer addTextRaw(String text) {
        getTop().addChild(this.new Comp().setTxtContent(text).doRaw());
        return this;
    }

    public Printer eos() {
        return addTextRaw(".").onLast(c -> c.setSentenceControl(SentenceControl.FORCE_OUT));
    }

    public Printer eos(String end) {
        return addTextRaw(end).onLast(c -> c.setSentenceControl(SentenceControl.FORCE_OUT));
    }

    public Printer sentenceForceOUT() {
        addComp().setSentenceControl(SentenceControl.FORCE_OUT);
        return this;
    }

    public Printer sentenceForceIN() {
        addComp().setSentenceControl(SentenceControl.FORCE_IN);
        return this;
    }

    public Printer addVerb(String verb, int number){
        VPPhraseSpec v = nlgFactory.createVerbPhrase(verb);
        setNumber(v, number);
        return addText(getSimpleNLGText(v));
    }

    public <T> Printer addVerb(String verb, Collection<T> c) {
        return addVerb(verb, c.size());
    }

    public Printer addVerbModal(String verb, Printer.Mode m){
        VPPhraseSpec v = nlgFactory.createVerbPhrase(verb);
        setModal(v, m);
        return addText(getSimpleNLGText(v));
    }


    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    // HTML Helpers Methods
    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

    // --- --- --- Titles

    public Printer addH1(String title) {
        return embedInTag("h1", title).onLast(Comp::doRaw).srcnl();
    }

    public Printer addH2(String title) {
        return embedInTag("h2", title).onLast(Comp::doRaw).srcnl();
    }

    public Printer addH3(String title) {
        return embedInTag("h3", title).onTop(Comp::doRaw).srcnl();
    }

    public Printer addH4(String title) {
        return embedInTag("h4", title).onTop(Comp::doRaw).srcnl();
    }

    public Printer addH5(String title) {
        return embedInTag("h5", title).onTop(Comp::doRaw).srcnl();
    }

    public Printer addH6(String title) {
        return embedInTag("h6", title).onTop(Comp::doRaw).srcnl();
    }



    // --- --- --- Paragraph, line break and text tools

    public Printer openPar() {
        return openTag("p").srcnl();
    }

    public Printer closePar() {
        return closeTag("p").srcnl();
    }

    public Printer lineBreak() {
        return addTextRaw("<br>").srcnl();
    }



    // --- --- --- Span

    // With a space before the span
    public Printer openSpan() {
        addComp().doRaw().setTxtContent(" ");
        openTag("span");
        return this;
    }

    public Printer closeSpan() {
        return closeTag("span");
    }



    // --- --- --- Table

    public Printer openTable() {
        return openTag("table").srcnl();
    }

    public Printer closeTable() {
        return closeTag("table").srcnl();
    }

    public Printer openTableRow() {
        return openTag("tr").srcnl();
    }

    public Printer closeTableRow() {
        return closeTag("tr").srcnl();
    }

    // --- Header part
    public Printer openTableHead() {
        return openTag("thead").srcnl();
    }

    public Printer closeTableHead() {
        return closeTag("thead").srcnl();
    }

    public Printer openTableHeader() {
        return openTag("th");
    }

    public Printer closeTableHeader() {
        return closeTag("th").srcnl();
    }

    public Printer tableHeader(String txt) {
        return embedInTag("th", txt).srcnl();
    }

    // --- Body part

    public Printer openTableBody() {
        return openTag("tbody").srcnl();
    }

    public Printer closeTableBody() {
        return closeTag("tbody").srcnl();
    }

    public Printer openTableData() {
        return openTag("td");
    }

    public Printer closeTableData() {
        return closeTag("td").srcnl();
    }

    public Printer tableData(String txt) {
        return embedInTag("td", txt).srcnl();
    }



    // --- --- --- Misc

    public Printer horizontalRule() {
        return addTextRaw("<hr>").srcnl();
    }

    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    // - HELPERS - HELPERS - HELPERS - HELPERS - HELPERS - HELPERS - HELPERS - HELPERS - HELPERS - HELPERS - HELPERS -
    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---


    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    // ENUM
    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

    // SentenceControl
    public enum SentenceControl {
        NO,
        FORCE_OUT,
        FORCE_IN
    }

    // SimpleNLG modals
    public enum Mode {
        CAN, MAY, MUST, OUGHT, SHALL, SHOULD, WILL, WOULD
    }


    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    // INNER TYPE
    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

    // HTML component
    public class Comp {


        // --- --- --- Fields

        public String tag = null;
        public Map<String, List<String>> attributes = null;

        public boolean doNL = false;
        public int indent = 0;
        public int AMOUNT_INDENT = 2;

        public boolean doRaw = false;
        public String txtContent = null;
        public SentenceControl sentenceControl = NO;

        public List<Comp> children = null;


        // --- --- --- Constructor

        public Comp() {
            // Account for the default container
            this.indent = (compStack.size() - 1) * AMOUNT_INDENT;
        }

        public Comp(String tag) {
            super();
            this.tag = tag;
        }


        // --- --- --- Manipulation methods

        public Comp setNL() {
            this.doNL = true;
            return this;
        }

        public Comp setTag(String tag) {
            this.tag = tag;
            return this;
        }

        public Comp addAttribute(String att, String value) {
            if (attributes == null) {
                attributes = new HashMap<>();
            }
            List<String> res = attributes.computeIfAbsent(att, k -> new ArrayList<>());
            res.add(value);
            return this;
        }

        public Comp setDoRaw(boolean doRaw) {
            this.doRaw = doRaw;
            return this;
        }

        public Comp doRaw() {
            return setDoRaw(true);
        }

        public Comp doNoRaw() {
            return setDoRaw(false);
        }

        public Comp setTxtContent(String txtContent) {
            this.txtContent = txtContent;
            return this;
        }

        public Comp setSentenceControl(SentenceControl sentenceControl) {
            this.sentenceControl = sentenceControl;
            return this;
        }

        public Comp addChild(Comp c) {
            if (children == null) {
                children = new ArrayList<>();
            }
            children.add(c);
            return this;
        }


        // --- --- --- Rendering

        // Complete rendering, taking sentence control into account
        public void render() {
            // If new line component, this is your only job!
            if (doNL) {
                sb.append("\n").append(pad(indent));
                return;
            }

            // --- --- -- Open Tag if not null
            renderOpenTag(sb);

            // --- --- --- Sentence control if needed
            switch (sentenceControl) {
                case NO:
                    break;
                case FORCE_IN:
                    inSentence = true;
                    break;
                case FORCE_OUT:
                    inSentence = false;
                    break;
            }

            // --- --- --- Text generation
            if (txtContent != null) {
                // --- DO NOT CONTROL TEXT
                if (doRaw) {
                    sb.append(txtContent);
                    forceSpace = false;
                } else {
                    // --- CONTROL TEXT
                    // Clean the string
                    String s = txtContent.trim();
                    if (!s.equals("")) {
                        // Add a space if needed.
                        int sbl = sb.length();
                        if (sbl > 0) {
                            char lastChar = sb.charAt(sbl - 1);
                            if (forceSpace || lastChar != ' ' && lastChar != '>') {
                                sb.append(' ');
                                forceSpace = false;
                            }
                        }
                        // Capitalize first letter if beginning of a sentence
                        if (inSentence) {
                            sb.append(s);
                        } else {
                            sb.append(capitalizeFirstLetter(s));
                            inSentence = true;
                        }
                    }
                }
            }

            // --- --- --- Children generation
            if (children != null) {
                // Tweak the last child if it is a newline: better indent.
                Comp last = children.get(children.size() - 1);
                if (last.doNL) {
                    last.indent -= AMOUNT_INDENT;
                }
                // Render
                for (Comp c : children) {
                    c.render();
                }
            }

            // --- --- --- Close tag
            renderCloseTag(sb);
        }

        private void renderOpenTag(StringBuilder sb) {
            if (tag != null) {
                sb.append('<').append(tag);
                if (attributes != null) {
                    for (Map.Entry<String, List<String>> e : attributes.entrySet()) {
                        sb.append(' ').append(e.getKey()).append("=\"");
                        for (String att : e.getValue()) {
                            sb.append(att).append(" ");
                        }
                        sb.append('\"');
                    }
                }
                sb.append('>');
            }
        }

        private void renderCloseTag(StringBuilder sb) {
            if (tag != null) {
                sb.append("</").append(tag).append(">");
                // Special support for space
                if(tag.equals("span")){
                    forceSpace = true;
                }

            }

        }

        // Simple rendering without control
        public String toString() {
            StringBuilder sb = new StringBuilder();
            renderOpenTag(sb);
            if (txtContent != null) {
                sb.append(txtContent);
            }
            if (children != null) {
                for (Comp c : children) {
                    sb.append(c.toString());
                }
            }
            renderCloseTag(sb);
            return sb.toString();
        }
    }


    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    // Source output
    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

    public Printer srcnl() {
        addComp().setNL();
        return this;
    }


    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    // String methods
    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

    /**
     * Generate n white space
     */
    public String pad(int n) {
        if (n < 0) {
            return "";
        }
        return String.join("", Collections.nCopies(n, " "));
    }

    /**
     * Capitalize the first letter of the string
     */
    public String capitalizeFirstLetter(String original) {
        if (original == null || original.length() == 0) {
            return original;
        }
        return original.substring(0, 1).toUpperCase() + original.substring(1);
    }


    /**
     * Add a comma separated list, with configurable last separating word (and, or...)
     */
    private String joinSepWithLast(List<String> ls, String sepWord) {

        List<String> nls = ls.stream().map(String::trim).collect(Collectors.toList());

        if (nls.size() == 0) {
            return "";
        } else if (nls.size() == 1) {
            return nls.get(0);
        } else if (nls.size() == 2) {
            String sep = sepWord.trim();
            return nls.get(0) + " " + sep + " " + nls.get(1);
        } else { // 3 and more
            StringBuilder s = new StringBuilder();
            String sep = sepWord.trim();

            // first item idx = 0
            s.append(nls.get(0));

            // middle items idx = [1, size-2]
            for (int i = 1; i < nls.size() - 1; ++i) {
                s.append(", ");
                s.append(nls.get(i));
            }

            // last item idx = size-1
            s.append(" ").append(sep).append(" ").append(nls.get(nls.size() - 1));
            return s.toString();
        }
    }


    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    // SimpleNLG stuff
    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

    // Generate SimpleNLGText. put in lower case, remove final dot.
    private static String getSimpleNLGText(NLGElement e) {
        String txt = realiser.realiseSentence(e).toLowerCase();
        return txt.substring(0, txt.length() - 1);
    }

    // --- --- --- SINGULAR/PLURAL

    private static void setNumber(NLGElement elem, int nb) {
        if (nb > 1) {
            elem.setFeature(Feature.NUMBER, NumberAgreement.PLURAL);
        } else {
            elem.setFeature(Feature.NUMBER, NumberAgreement.SINGULAR);
        }
    }

    private static <T> void setNumber(NLGElement elem, Collection<T> c) {
        setNumber(elem, c.size());
    }


    // --- --- --- Modal

    private static void setModal(NLGElement elem, Mode mode) {
        switch (mode) {
            case CAN:
                elem.setFeature(Feature.MODAL, "can");
                break;
            case MAY:
                elem.setFeature(Feature.MODAL, "may");
                break;
            case MUST:
                elem.setFeature(Feature.MODAL, "must");
                break;
            case OUGHT:
                elem.setFeature(Feature.MODAL, "ought");
                break;
            case SHALL:
                elem.setFeature(Feature.MODAL, "shall");
                break;
            case SHOULD:
                elem.setFeature(Feature.MODAL, "should");
                break;
            case WILL:
                elem.setFeature(Feature.MODAL, "will");
                break;
            case WOULD:
                elem.setFeature(Feature.MODAL, "would");
                break;
            default:
                shouldNotHappen("Should not happen");
        }
    }

}
