package com.openai.sigilbound;

import android.graphics.Color;
import android.graphics.PointF;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Data-driven spell language for Sigilbound v2.0.
 *
 * A spell is compiled from ordered elemental sigils, one form rune, and ordered
 * behavior clauses. Elements contribute continuous channels; forms and clauses
 * interpret those channels. Runtime reactions compare channels instead of
 * selecting bespoke element-pair recipes, and persistent objects (fields,
 * traps, wards, glyphs, rifts, and auras) keep modifying each other after they
 * resolve.
 */
final class SpellSystem {
    private SpellSystem() {}

    enum Element {
        FIRE("Fire", "triangle", Color.rgb(255, 91, 60), "heat • ignition"),
        WATER("Water", "wave", Color.rgb(71, 194, 228), "moisture • damping"),
        WIND("Wind", "spiral", Color.rgb(126, 231, 178), "impulse • spread"),
        STONE("Stone", "square", Color.rgb(214, 165, 82), "mass • structure"),
        FROST("Frost", "diamond", Color.rgb(151, 220, 255), "cold • brittleness"),
        LIGHTNING("Lightning", "bolt", Color.rgb(244, 220, 92), "charge • velocity"),
        AETHER("Aether", "circle", Color.rgb(184, 128, 255), "resonance • linking"),
        VOID("Void", "cross", Color.rgb(226, 78, 170), "entropy • consumption"),
        RADIANCE("Radiance", "arch", Color.rgb(255, 243, 196), "revelation • searing light"),
        VERDANCE("Verdance", "sprout", Color.rgb(105, 208, 92), "growth • entanglement");

        final String label;
        final String glyphHint;
        final int color;
        final String subtitle;

        Element(String label, String glyphHint, int color, String subtitle) {
            this.label = label;
            this.glyphHint = glyphHint;
            this.color = color;
            this.subtitle = subtitle;
        }
    }

    enum Form {
        LANCE("Lance", "traveling interceptor", 12f),
        WARD("Ward", "persistent barrier", 16f),
        ORBIT("Orbit", "repeating familiar", 19f),
        BURST("Burst", "delayed field event", 21f),
        BEAM("Beam", "fast piercing channel", 18f),
        GLYPH("Glyph", "ley-field inscription", 17f),
        SURGE("Surge", "wavefront that shoves", 15f),
        RIFT("Rift", "lane-rewriting portal", 20f),
        AURA("Aura", "global enchantment", 22f);

        final String label;
        final String subtitle;
        final float baseCost;

        Form(String label, String subtitle, float baseCost) {
            this.label = label;
            this.subtitle = subtitle;
            this.baseCost = baseCost;
        }
    }

    enum Clause {
        ECHO("Echo", "repeat after delay", 5f),
        FORK("Fork", "branch adjacent lanes", 6f),
        ANCHOR("Anchor", "heavier and persistent", 5f),
        SEEK("Seek", "retarget at mid-field", 5f),
        RELAY("Relay", "route through ley nodes", 6f),
        TRIGGER("Trigger", "wait for hostile entry", 5f),
        BIND("Bind", "enchant an allied spell", 6f),
        CONSUME("Consume", "sacrifice field for power", 4f),
        SWIFT("Swift", "faster, leaner body", 4f),
        SIPHON("Siphon", "drain what it crosses", 6f),
        HEX("Hex", "curse a hostile object", 6f),
        DISPEL("Dispel", "erase a hostile spell", 5f);

        final String label;
        final String subtitle;
        final float cost;

        Clause(String label, String subtitle, float cost) {
            this.label = label;
            this.subtitle = subtitle;
            this.cost = cost;
        }
    }

    enum Artifact {
        PRISM_LENS("Prism Lens", "later sigils retain more influence"),
        ASHEN_QUILL("Ashen Quill", "clean glyphs surge and restore Ink"),
        AEGIS_BELL("Aegis Bell", "wards and familiars are reinforced"),
        LEY_KEY("Ley Key", "nodes charge faster; Relay is cheaper"),
        MNEMONIC_CROWN("Mnemonic Crown", "executables cost less mana, restore less Ink"),
        HEXWRIGHT_RING("Hexwright Ring", "Hex, Dispel, and Siphon strike deeper"),
        VERDANT_CHALICE("Verdant Chalice", "growth surges; overgrowth mends the caster");

        final String label;
        final String subtitle;

        Artifact(String label, String subtitle) {
            this.label = label;
            this.subtitle = subtitle;
        }
    }

    enum Difficulty {
        APPRENTICE("Apprentice", "long tells • simple syntax", 1.45f, 0.72f),
        ADEPT("Adept", "measured rival", 1.05f, 1.0f),
        ARCHMAGE("Archmage", "dense syntax • reactive", 0.78f, 1.22f);

        final String label;
        final String subtitle;
        final float channelScale;
        final float pressureScale;

        Difficulty(String label, String subtitle, float channelScale, float pressureScale) {
            this.label = label;
            this.subtitle = subtitle;
            this.channelScale = channelScale;
            this.pressureScale = pressureScale;
        }
    }

    enum Tempo {
        RITUAL("Ritual", "slow and readable", 0.42f, 1.28f),
        DUEL("Duel", "balanced cadence", 0.62f, 1.0f),
        BLITZ("Blitz", "high-pressure casting", 0.84f, 0.82f);

        final String label;
        final String subtitle;
        final float projectileScale;
        final float channelScale;

        Tempo(String label, String subtitle, float projectileScale, float channelScale) {
            this.label = label;
            this.subtitle = subtitle;
            this.projectileScale = projectileScale;
            this.channelScale = channelScale;
        }
    }

    enum ArenaType {
        ASTRAL_COURT("Astral Court", "balanced ley nodes amplify coherent syntax",
                Color.rgb(83, 63, 124), Color.rgb(40, 29, 70)),
        EMBER_VAULT("Ember Vault", "heat vents empower ignition and molten residue",
                Color.rgb(143, 63, 44), Color.rgb(62, 25, 27)),
        TIDAL_ARCHIVE("Tidal Archive", "water channels slow travel and conduct charge",
                Color.rgb(42, 102, 132), Color.rgb(20, 48, 70)),
        SHATTERED_CROWN("Shattered Crown", "fractured bridges favor forks and unstable impacts",
                Color.rgb(115, 79, 126), Color.rgb(46, 31, 58)),
        VERDANT_REACH("Verdant Reach", "living ley roots mend constructs and entangle spells",
                Color.rgb(66, 124, 70), Color.rgb(24, 50, 32)),
        RADIANT_BASILICA("Radiant Basilica", "shafts of consecrated light empower beams and burst casts",
                Color.rgb(158, 126, 62), Color.rgb(66, 52, 26));

        final String label;
        final String subtitle;
        final int accent;
        final int shadow;

        ArenaType(String label, String subtitle, int accent, int shadow) {
            this.label = label;
            this.subtitle = subtitle;
            this.accent = accent;
            this.shadow = shadow;
        }
    }

    enum Kind {
        PROJECTILE, WARD, ORBIT, BURST, BEAM, GLYPH, FIELD, TRAP, ENCHANTMENT, SHARD, SURGE, RIFT, AURA
    }

    static final class Program {
        String name = "Untitled Formula";
        final ArrayList<Element> elements = new ArrayList<Element>();
        Form form = Form.LANCE;
        final ArrayList<Clause> clauses = new ArrayList<Clause>();

        Program() {}

        Program(List<Element> elements, Form form, List<Clause> clauses) {
            this.elements.addAll(elements);
            this.form = form;
            this.clauses.addAll(clauses);
            this.name = autoName(this);
        }

        Program copy() {
            Program p = new Program(elements, form, clauses);
            p.name = name;
            return p;
        }

        boolean valid() {
            return !elements.isEmpty() && form != null;
        }

        String shortLabel() {
            StringBuilder b = new StringBuilder();
            for (Element e : elements) {
                if (b.length() > 0) b.append('·');
                b.append(e.label.charAt(0));
            }
            if (b.length() == 0) b.append('?');
            return b + " " + form.label;
        }

        boolean has(Clause clause) {
            return clauses.contains(clause);
        }
    }

    static final class Profile {
        float heat;
        float moisture;
        float impulse;
        float mass;
        float cohesion;
        float volatility;
        float cold;
        float charge;
        float aether;
        float entropy;
        float radiance;
        float growth;

        float steam;
        float magma;
        float shatter;
        float conduction;
        float blizzard;
        float nullFlux;
        float corrosion;
        float resonance;
        float thermalShock;
        float wildfire;
        float overgrowth;
        float sanctify;
        float eclipse;

        String label = "";

        Profile copy() {
            Profile p = new Profile();
            p.heat = heat;
            p.moisture = moisture;
            p.impulse = impulse;
            p.mass = mass;
            p.cohesion = cohesion;
            p.volatility = volatility;
            p.cold = cold;
            p.charge = charge;
            p.aether = aether;
            p.entropy = entropy;
            p.radiance = radiance;
            p.growth = growth;
            p.recalculate();
            p.label = label;
            return p;
        }

        void blend(Profile other, float amount) {
            if (other == null || amount <= 0f) return;
            heat += other.heat * amount;
            moisture += other.moisture * amount;
            impulse += other.impulse * amount;
            mass += other.mass * amount;
            cohesion += other.cohesion * amount;
            volatility += other.volatility * amount;
            cold += other.cold * amount;
            charge += other.charge * amount;
            aether += other.aether * amount;
            entropy += other.entropy * amount;
            radiance += other.radiance * amount;
            growth += other.growth * amount;
            recalculate();
        }

        void scale(float scale) {
            heat *= scale;
            moisture *= scale;
            impulse *= scale;
            mass *= scale;
            cohesion *= scale;
            volatility *= scale;
            cold *= scale;
            charge *= scale;
            aether *= scale;
            entropy *= scale;
            radiance *= scale;
            growth *= scale;
            recalculate();
        }

        float force() {
            return 1f + heat * 0.12f + impulse * 0.14f + mass * 0.13f
                    + charge * 0.14f + volatility * 0.08f + aether * 0.06f
                    + radiance * 0.11f + growth * 0.03f;
        }

        float stability() {
            return Math.max(0.35f, 1f + cohesion * 0.24f + mass * 0.08f + growth * 0.10f
                    + radiance * 0.04f - volatility * 0.12f - entropy * 0.10f);
        }

        String dominantReaction() {
            float best = 0.22f;
            String result = "Resonance";
            if (steam > best) { best = steam; result = "Steam"; }
            if (magma > best) { best = magma; result = "Magma"; }
            if (shatter > best) { best = shatter; result = "Shatter"; }
            if (conduction > best) { best = conduction; result = "Conduction"; }
            if (blizzard > best) { best = blizzard; result = "Blizzard"; }
            if (nullFlux > best) { best = nullFlux; result = "Null Flux"; }
            if (corrosion > best) { best = corrosion; result = "Corrosion"; }
            if (wildfire > best) { best = wildfire; result = "Wildfire"; }
            if (overgrowth > best) { best = overgrowth; result = "Overgrowth"; }
            if (sanctify > best) { best = sanctify; result = "Sanctify"; }
            if (eclipse > best) { best = eclipse; result = "Eclipse"; }
            if (thermalShock > best) result = "Thermal Shock";
            return result;
        }

        void recalculate() {
            heat = Math.max(0f, heat);
            moisture = Math.max(0f, moisture);
            impulse = Math.max(0.04f, impulse);
            mass = Math.max(0f, mass);
            cohesion = Math.max(0f, cohesion);
            volatility = Math.max(0f, volatility);
            cold = Math.max(0f, cold);
            charge = Math.max(0f, charge);
            aether = Math.max(0f, aether);
            entropy = Math.max(0f, entropy);
            radiance = Math.max(0f, radiance);
            growth = Math.max(0f, growth);

            steam = Math.max(0f, heat * moisture - 0.24f);
            magma = Math.max(0f, heat * mass - 0.34f);
            shatter = Math.max(0f, cold * mass + mass * impulse * 0.44f - 0.36f);
            conduction = Math.max(0f, charge * moisture - 0.20f);
            blizzard = Math.max(0f, cold * impulse - 0.26f);
            nullFlux = Math.max(0f, aether * entropy - 0.24f);
            corrosion = Math.max(0f, entropy * (moisture + heat * 0.30f) - 0.20f);
            resonance = Math.max(0f, aether * (cohesion + charge * 0.32f));
            thermalShock = Math.max(0f, heat * cold - 0.22f);
            wildfire = Math.max(0f, heat * growth - 0.24f);
            overgrowth = Math.max(0f, growth * (moisture + cohesion * 0.30f) - 0.22f);
            sanctify = Math.max(0f, radiance * (cohesion + aether * 0.40f) - 0.24f);
            eclipse = Math.max(0f, radiance * entropy - 0.18f);
        }
    }

    static Profile compile(List<Element> elements, Artifact artifact, ArenaType arena) {
        Profile p = new Profile();
        float[] baseWeights = {1f, 0.82f, 0.66f, 0.52f};
        StringBuilder label = new StringBuilder();
        for (int i = 0; i < elements.size() && i < 4; i++) {
            Element e = elements.get(i);
            float weight = baseWeights[i];
            if (artifact == Artifact.PRISM_LENS && i > 0) weight += 0.14f;
            addElement(p, e, weight);
            if (label.length() > 0) label.append('·');
            label.append(e.label.toUpperCase(Locale.US));
        }

        switch (arena) {
            case EMBER_VAULT:
                p.heat += 0.20f;
                p.volatility += 0.08f;
                break;
            case TIDAL_ARCHIVE:
                p.moisture += 0.20f;
                p.cohesion += 0.06f;
                break;
            case SHATTERED_CROWN:
                p.impulse += 0.10f;
                p.volatility += 0.10f;
                break;
            case VERDANT_REACH:
                p.growth += 0.20f;
                p.moisture += 0.08f;
                break;
            case RADIANT_BASILICA:
                p.radiance += 0.20f;
                p.cohesion += 0.06f;
                break;
            case ASTRAL_COURT:
            default:
                p.aether += 0.10f;
                p.cohesion += 0.04f;
                break;
        }
        if (artifact == Artifact.VERDANT_CHALICE) p.growth += 0.10f;
        p.label = label.toString();
        p.recalculate();
        return p;
    }

    private static void addElement(Profile p, Element e, float w) {
        switch (e) {
            case FIRE:
                p.heat += 1.00f * w;
                p.volatility += 0.50f * w;
                p.impulse += 0.20f * w;
                break;
            case WATER:
                p.moisture += 1.00f * w;
                p.cohesion += 0.30f * w;
                p.heat -= 0.18f * w;
                break;
            case WIND:
                p.impulse += 1.00f * w;
                p.volatility += 0.25f * w;
                p.cohesion -= 0.08f * w;
                break;
            case STONE:
                p.mass += 1.00f * w;
                p.cohesion += 0.72f * w;
                p.impulse -= 0.06f * w;
                break;
            case FROST:
                p.cold += 1.00f * w;
                p.cohesion += 0.34f * w;
                p.volatility += 0.12f * w;
                p.heat -= 0.42f * w;
                break;
            case LIGHTNING:
                p.charge += 1.00f * w;
                p.impulse += 0.54f * w;
                p.volatility += 0.46f * w;
                break;
            case AETHER:
                p.aether += 1.00f * w;
                p.cohesion += 0.26f * w;
                p.charge += 0.18f * w;
                break;
            case VOID:
                p.entropy += 1.00f * w;
                p.volatility += 0.56f * w;
                p.cohesion -= 0.30f * w;
                p.moisture -= 0.06f * w;
                break;
            case RADIANCE:
                p.radiance += 1.00f * w;
                p.heat += 0.24f * w;
                p.cohesion += 0.20f * w;
                p.entropy -= 0.30f * w;
                break;
            case VERDANCE:
                p.growth += 1.00f * w;
                p.moisture += 0.30f * w;
                p.cohesion += 0.32f * w;
                p.volatility -= 0.10f * w;
                break;
        }
    }

    static float manaCost(Program p, Artifact artifact) {
        if (p == null || p.form == null) return 0f;
        float cost = p.form.baseCost + Math.max(0, p.elements.size() - 1) * 5.2f;
        for (Clause clause : p.clauses) cost += clause.cost;
        if (artifact == Artifact.LEY_KEY && p.clauses.contains(Clause.RELAY)) cost -= 3f;
        if (artifact == Artifact.AEGIS_BELL && (p.form == Form.WARD || p.form == Form.ORBIT)) cost += 1.5f;
        if (artifact == Artifact.HEXWRIGHT_RING) {
            if (p.clauses.contains(Clause.HEX)) cost -= 1f;
            if (p.clauses.contains(Clause.DISPEL)) cost -= 1f;
            if (p.clauses.contains(Clause.SIPHON)) cost -= 1f;
        }
        return Math.max(8f, cost);
    }

    static float executableSurcharge(Artifact artifact) {
        return artifact == Artifact.MNEMONIC_CROWN ? 1.10f : 1.25f;
    }

    static String autoName(Program p) {
        if (p == null || p.elements.isEmpty()) return "Untitled Formula";
        StringBuilder b = new StringBuilder();
        int shown = Math.min(2, p.elements.size());
        for (int i = 0; i < shown; i++) {
            if (i > 0) b.append(' ');
            b.append(p.elements.get(i).label);
        }
        if (p.elements.size() > 2) b.append(" Triune");
        b.append(' ').append(p.form.label);
        if (!p.clauses.isEmpty()) b.append(" · ").append(p.clauses.get(0).label);
        return b.toString();
    }

    static String encode(Program p) {
        StringBuilder elements = new StringBuilder();
        for (Element e : p.elements) {
            if (elements.length() > 0) elements.append(',');
            elements.append(e.name());
        }
        StringBuilder clauses = new StringBuilder();
        for (Clause clause : p.clauses) {
            if (clauses.length() > 0) clauses.append(',');
            clauses.append(clause.name());
        }
        String safeName = p.name == null ? autoName(p) : p.name.replace("|", " ").replace("\n", " ");
        return safeName + "|" + elements + "|" + p.form.name() + "|" + clauses;
    }

    static Program decode(String encoded, Program fallback) {
        if (encoded == null || encoded.length() == 0) return fallback == null ? new Program() : fallback.copy();
        try {
            String[] parts = encoded.split("\\|", -1);
            Program p = new Program();
            p.name = parts.length > 0 && parts[0].length() > 0 ? parts[0] : "Untitled Formula";
            if (parts.length > 1 && parts[1].length() > 0) {
                for (String token : parts[1].split(",")) {
                    if (p.elements.size() >= 4) break;
                    p.elements.add(Element.valueOf(token));
                }
            }
            p.form = parts.length > 2 && parts[2].length() > 0 ? Form.valueOf(parts[2]) : Form.LANCE;
            if (parts.length > 3 && parts[3].length() > 0) {
                for (String token : parts[3].split(",")) {
                    if (p.clauses.size() >= 3) break;
                    p.clauses.add(Clause.valueOf(token));
                }
            }
            if (p.elements.isEmpty()) return fallback == null ? p : fallback.copy();
            return p;
        } catch (RuntimeException ignored) {
            return fallback == null ? new Program() : fallback.copy();
        }
    }

    static Program program(Element[] elements, Form form, Clause[] clauses) {
        Program p = new Program();
        Collections.addAll(p.elements, elements);
        p.form = form;
        Collections.addAll(p.clauses, clauses);
        p.name = autoName(p);
        return p;
    }

    static final class GlyphResult {
        final Element element;
        final float score;

        GlyphResult(Element element, float score) {
            this.element = element;
            this.score = score;
        }
    }

    /** Lightweight unistroke recognizer inspired by the $1 recognizer. */
    static final class GlyphRecognizer {
        private static final int SAMPLE_COUNT = 56;
        private final ArrayList<Template> templates = new ArrayList<Template>();

        GlyphRecognizer() {
            templates.add(template(Element.FIRE, new float[][]{
                    {0.5f, 0f}, {1f, 1f}, {0f, 1f}, {0.5f, 0f}}));
            templates.add(template(Element.WATER, new float[][]{
                    {0f, 0.52f}, {0.10f, 0.18f}, {0.20f, 0.50f}, {0.30f, 0.82f},
                    {0.40f, 0.18f}, {0.50f, 0.50f}, {0.60f, 0.82f}, {0.70f, 0.18f},
                    {0.80f, 0.50f}, {0.90f, 0.76f}, {1f, 0.52f}}));
            templates.add(template(Element.WIND, new float[][]{
                    {0.02f, 0.80f}, {0.14f, 0.40f}, {0.38f, 0.08f}, {0.70f, 0.10f},
                    {0.94f, 0.38f}, {0.86f, 0.72f}, {0.58f, 0.90f}, {0.30f, 0.70f},
                    {0.32f, 0.40f}, {0.55f, 0.28f}, {0.72f, 0.48f}, {0.56f, 0.66f},
                    {0.43f, 0.53f}}));
            templates.add(template(Element.STONE, new float[][]{
                    {0f, 0f}, {1f, 0f}, {1f, 1f}, {0f, 1f}, {0f, 0f}}));
            templates.add(template(Element.FROST, new float[][]{
                    {0.5f, 0f}, {1f, 0.5f}, {0.5f, 1f}, {0f, 0.5f}, {0.5f, 0f}, {0.5f, 1f}}));
            templates.add(template(Element.LIGHTNING, new float[][]{
                    {0.72f, 0f}, {0.28f, 0.44f}, {0.62f, 0.44f}, {0.24f, 1f}}));
            templates.add(template(Element.AETHER, circlePoints()));
            templates.add(template(Element.VOID, new float[][]{
                    {0f, 0f}, {1f, 1f}, {0.52f, 0.52f}, {1f, 0f}, {0f, 1f}}));
            templates.add(template(Element.RADIANCE, new float[][]{
                    {0f, 1f}, {0.04f, 0.52f}, {0.22f, 0.10f}, {0.5f, 0f},
                    {0.78f, 0.10f}, {0.96f, 0.52f}, {1f, 1f}}));
            templates.add(template(Element.VERDANCE, new float[][]{
                    {0f, 0.48f}, {0.34f, 1f}, {1f, 0f}}));
        }

        GlyphResult recognize(List<PointF> raw) {
            if (raw == null || raw.size() < 3) return new GlyphResult(Element.FIRE, 0f);
            List<PointF> candidate = normalize(resample(raw, SAMPLE_COUNT));
            ArrayList<PointF> reverseSource = new ArrayList<PointF>();
            for (PointF p : raw) reverseSource.add(new PointF(p.x, p.y));
            Collections.reverse(reverseSource);
            List<PointF> reversed = normalize(resample(reverseSource, SAMPLE_COUNT));
            Template best = templates.get(0);
            float bestDistance = Float.MAX_VALUE;
            for (Template t : templates) {
                float distance = Math.min(pathDistance(candidate, t.points), pathDistance(reversed, t.points));
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = t;
                }
            }
            float score = 1f - bestDistance / 0.74f;
            return new GlyphResult(best.element, clamp(score, 0f, 1f));
        }

        private static Template template(Element element, float[][] anchors) {
            return new Template(element, normalize(resample(polyline(anchors), SAMPLE_COUNT)));
        }

        private static float[][] circlePoints() {
            float[][] points = new float[25][2];
            for (int i = 0; i < points.length; i++) {
                float angle = (float) (-Math.PI / 2.0 + Math.PI * 2.0 * i / (points.length - 1));
                points[i][0] = 0.5f + 0.5f * (float) Math.cos(angle);
                points[i][1] = 0.5f + 0.5f * (float) Math.sin(angle);
            }
            return points;
        }

        private static ArrayList<PointF> polyline(float[][] anchors) {
            ArrayList<PointF> result = new ArrayList<PointF>();
            for (float[] anchor : anchors) result.add(new PointF(anchor[0], anchor[1]));
            return result;
        }

        private static List<PointF> resample(List<PointF> points, int count) {
            ArrayList<PointF> source = new ArrayList<PointF>();
            for (PointF p : points) source.add(new PointF(p.x, p.y));
            if (source.size() == 1) {
                ArrayList<PointF> repeated = new ArrayList<PointF>();
                for (int i = 0; i < count; i++) repeated.add(new PointF(source.get(0).x, source.get(0).y));
                return repeated;
            }
            float total = 0f;
            for (int i = 1; i < source.size(); i++) total += distance(source.get(i - 1), source.get(i));
            float interval = total / Math.max(1, count - 1);
            float accumulated = 0f;
            ArrayList<PointF> output = new ArrayList<PointF>();
            output.add(new PointF(source.get(0).x, source.get(0).y));
            int index = 1;
            while (index < source.size() && output.size() < count) {
                PointF previous = source.get(index - 1);
                PointF current = source.get(index);
                float segment = distance(previous, current);
                if (segment <= 0.0001f) {
                    index++;
                    continue;
                }
                if (accumulated + segment >= interval) {
                    float t = (interval - accumulated) / segment;
                    PointF q = new PointF(previous.x + t * (current.x - previous.x),
                            previous.y + t * (current.y - previous.y));
                    output.add(q);
                    source.add(index, q);
                    accumulated = 0f;
                    index++;
                } else {
                    accumulated += segment;
                    index++;
                }
            }
            while (output.size() < count) {
                PointF last = source.get(source.size() - 1);
                output.add(new PointF(last.x, last.y));
            }
            return output;
        }

        private static List<PointF> normalize(List<PointF> points) {
            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;
            for (PointF p : points) {
                minX = Math.min(minX, p.x);
                minY = Math.min(minY, p.y);
                maxX = Math.max(maxX, p.x);
                maxY = Math.max(maxY, p.y);
            }
            float width = Math.max(0.001f, maxX - minX);
            float height = Math.max(0.001f, maxY - minY);
            ArrayList<PointF> output = new ArrayList<PointF>();
            float centerX = 0f;
            float centerY = 0f;
            for (PointF p : points) {
                PointF q = new PointF((p.x - minX) / width, (p.y - minY) / height);
                output.add(q);
                centerX += q.x;
                centerY += q.y;
            }
            centerX /= output.size();
            centerY /= output.size();
            for (PointF p : output) {
                p.x -= centerX;
                p.y -= centerY;
            }
            return output;
        }

        private static float pathDistance(List<PointF> a, List<PointF> b) {
            int count = Math.min(a.size(), b.size());
            float total = 0f;
            for (int i = 0; i < count; i++) total += distance(a.get(i), b.get(i));
            return total / Math.max(1, count);
        }

        private static float distance(PointF a, PointF b) {
            float dx = b.x - a.x;
            float dy = b.y - a.y;
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

        private static float clamp(float value, float minimum, float maximum) {
            return Math.max(minimum, Math.min(maximum, value));
        }

        private static final class Template {
            final Element element;
            final List<PointF> points;

            Template(Element element, List<PointF> points) {
                this.element = element;
                this.points = points;
            }
        }
    }
}
