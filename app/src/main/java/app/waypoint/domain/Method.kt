package app.waypoint.domain

/** Where a method sits on the way to a goal. */
enum class Group {
    /** Tools that help pick a goal before there is one. */
    CHOOSE,
    /** Lenses that sharpen a goal: wording, motivation, obstacles. */
    SHAPE,
    /** Structures that break a goal down and drive it day to day. */
    DRIVE,
    /** Lenses that protect a goal from failing. */
    PROTECT,
}

/**
 * Every method in the app. [DRIVE] methods are a goal's engine (exactly one per goal);
 * [SHAPE] and [PROTECT] are lenses any goal can add; [CHOOSE] tools stand on their own and can spawn goals.
 */
enum class Method(val group: Group) {
    // Drive: one per goal
    MANDALA(Group.DRIVE),
    OKR(Group.DRIVE),
    WEEKS12(Group.DRIVE),
    FOURDX(Group.DRIVE),
    KAIZEN(Group.DRIVE),
    PACT(Group.DRIVE),
    LADDER(Group.DRIVE),
    SIMPLE(Group.DRIVE),
    TRACKER(Group.DRIVE),

    // Lenses
    SMART(Group.SHAPE),
    WOOP(Group.SHAPE),
    GROW(Group.SHAPE),
    WHYS(Group.SHAPE),
    PREMORTEM(Group.PROTECT),

    // Choosing tools
    WHEEL(Group.CHOOSE),
    ODYSSEY(Group.CHOOSE),
    BUFFETT(Group.CHOOSE),
    DREAMLINE(Group.CHOOSE);

    val drives: Boolean get() = group == Group.DRIVE
    val lens: Boolean get() = group == Group.SHAPE || group == Group.PROTECT
    val tool: Boolean get() = group == Group.CHOOSE

    companion object {
        fun of(name: String?): Method = entries.firstOrNull { it.name == name } ?: SIMPLE
        val drivers = entries.filter { it.drives }
        val lenses = entries.filter { it.lens }
        val tools = entries.filter { it.tool }
    }
}

/** What a [app.waypoint.data.Node] is. */
enum class NodeKind {
    /** Mandala: one of the eight themes around the goal. */
    PILLAR,
    /** A concrete action or habit (mandala cell, OKR initiative, GROW "will" step, simple goal habit). */
    ACTION,
    /** OKR key result: a number from start to target. */
    KR,
    /** Progress-tracker task: a counted number from start to target, bumped with + / −. */
    TASK,
    /** 12-week year tactic, tied to a week. */
    TACTIC,
    /** 4DX lead measure: counted per week against a target. */
    LEAD,
    /** Reverse-planning step with a due date. */
    STEP,
    /** A one-off checkpoint of a simple goal. */
    MILESTONE,
    /** If-then plan: title is the "if", note is the "then" (WOOP and pre-mortem). */
    PLAN,
    /** Pre-mortem: title is how it could fail, note how to prevent it. */
    RISK,
    /** GROW: an option; done = chosen. */
    OPTION;

    companion object {
        fun of(name: String): NodeKind? = entries.firstOrNull { it.name == name }
    }
}

/** The eight areas of the wheel of life. */
enum class Area {
    HEALTH, CAREER, MONEY, LOVE, FAMILY, GROWTH, FUN, HOME;

    companion object {
        fun of(i: Int): Area? = entries.getOrNull(i)
    }
}
