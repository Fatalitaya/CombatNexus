// Typed server identity, admission and callback lifetime on the live server.
(function () {
    function cls(name) { return Java.loadClass("io.github.fatalitaya.combatnexus.combat." + name); }
    var checks = [];
    function check(value, label) { if (!value) throw new Error(label); checks.push(label); }
    function rejects(action, label) { var rejected = false; try { action(); } catch (error) { rejected = true; } check(rejected, label); }
    var Session = cls("api.transaction.ServerSession"), Lineage = cls("api.transaction.CombatLineage");
    var Operation = cls("api.transaction.CombatOperation"), Dispatch = cls("internal.transaction.CombatDispatchScope");
    var Execution = cls("internal.transaction.CombatExecutionScope"), Set = Java.loadClass("java.util.Set");
    var kernel = cls("internal.transaction.CombatServerRuntime").existing(AutoJS.server()).orElseThrow();
    var session = kernel.session(), other = new Session(), root = Lineage.root(Operation.HEALING);
    check(root.bindServer(session), "root binds owning session");
    check(root.bindServer(session), "same session binding is stable");
    check(!root.bindServer(other), "another session cannot rebind root");
    check(!root.child(Operation.DAMAGE).bindServer(other), "descendant retains server identity");
    rejects(function () { root.bindServer(null); }, "null server identity rejected");
    var owner = new (Java.loadClass("java.lang.Object"))(), delivery = new (Java.loadClass("java.lang.Object"))();
    var parent, scope = Dispatch.callback(owner, delivery, null, session, root, Set.of(Operation.HEALING));
    try {
        parent = Dispatch.parentFor(delivery, session).orElseThrow();
        check(Dispatch.parentFor(delivery, other).isEmpty(), "foreign session cannot obtain callback authority");
        check(parent.permits(session, Operation.HEALING), "callback allows declared operation");
        check(!parent.permits(other, Operation.HEALING), "foreign session cannot use callback authority");
        check(!parent.permits(session, Operation.DAMAGE), "callback permissions remain narrowed");
        rejects(function () { parent.child(other, Operation.HEALING); }, "foreign child submission rejected");
        var restriction = Dispatch.restrict(Set.of());
        try { check(!parent.permits(session, Operation.HEALING), "nested restriction cannot widen permissions"); }
        finally { restriction.close(); }
        check(parent.child(session, Operation.HEALING).bindServer(session), "authorized child retains session");
    } finally { scope.close(); }
    check(!parent.permits(session, Operation.HEALING), "retained callback expires after delivery");
    var admitted = Lineage.root(Operation.HEALING);
    check(kernel.admit(admitted).admitted(), "live server admits transaction");
    var ticket = kernel.ticket(admitted);
    try {
        check(ticket.serverIdentity() === session, "ticket exposes business session");
        var application = ticket.enter(Execution.Phase.APPLICATION);
        try {
            check(Execution.currentLineage(session).orElseThrow().equals(admitted), "application resolves same server lineage");
            rejects(function () { Execution.currentLineage(other); }, "nested foreign server cannot borrow application");
        } finally { application.close(); }
        check(Execution.currentLineage(session).isEmpty(), "application close clears lineage");
    } finally { kernel.finish(admitted); kernel.drain(); }
    rejects(function () { ticket.requireActive(); }, "retained admission ticket expires");
    var level = AutoJS.server().overworld(), Identity = cls("api.transaction.ParticipantIdentity");
    var EntityType = Java.loadClass("net.minecraft.world.entity.EntityType"), Spawn = Java.loadClass("net.minecraft.world.entity.EntitySpawnReason");
    var first = EntityType.COW.create(level, Spawn.COMMAND), second = EntityType.COW.create(level, Spawn.COMMAND);
    second.setUUID(first.getUUID());
    var firstIdentity = new Identity(first.getUUID(), first.combatnexus$incarnation());
    var secondIdentity = new Identity(second.getUUID(), second.combatnexus$incarnation());
    check(!firstIdentity.equals(secondIdentity), "same UUID native instances have distinct incarnation");
    check(firstIdentity.equals(new Identity(first.getUUID(), first.combatnexus$incarnation())), "exact unspawned instance keeps identity");
    var instanceLineage = Lineage.root(Operation.DAMAGE);
    check(kernel.admit(instanceLineage).admitted(), "instance scope admitted");
    try {
        var instanceTicket = kernel.ticket(instanceLineage), worldSession = level.combatnexus$worldSession();
        instanceTicket.bindSession(worldSession, firstIdentity);
        instanceTicket.bindSession(worldSession, firstIdentity);
        rejects(function () { instanceTicket.bindSession(worldSession, secondIdentity); }, "ticket refuses same UUID replacement instance");
        var otherWorld = new (cls("api.transaction.WorldSession"))(session, worldSession.dimension());
        rejects(function () { instanceTicket.bindSession(otherWorld, firstIdentity); }, "ticket refuses distinct world session for same dimension");
    } finally { kernel.finish(instanceLineage); kernel.drain(); first.discard(); second.discard(); }
    var missingLineage=Lineage.root(Operation.SHIELD),UUID=Java.loadClass("java.util.UUID");
    check(kernel.admit(missingLineage).admitted(),"missing target scope admitted for no-op command");
    try {
        var missingTicket=kernel.ticket(missingLineage),world=cls("internal.platform.PlatformWorldLifecycle").existing(level);
        missingTicket.bindTarget(world,UUID.randomUUID());
        rejects(function(){missingTicket.bindTarget(world,UUID.randomUUID());},"missing target cannot be rebound to another UUID");
    } finally {kernel.finish(missingLineage);kernel.drain();}
    var Kernel = cls("internal.transaction.CombatServerRuntime");
    var Bool = Java.loadClass("java.util.function.BooleanSupplier"), Int = Java.loadClass("java.util.function.IntSupplier");
    var clock = 4;
    var thread = new Bool({getAsBoolean: function () { return true; }});
    var ticks = new Int({getAsInt: function () { return clock; }});
    var isolated = new Kernel(new Session(), thread, ticks), foreign = new Kernel(new Session(), thread, ticks);
    var bound = Lineage.root(Operation.HEALING), retained;
    try {
        check(isolated.admit(bound).admitted(), "injected kernel admits without native server");
        retained = isolated.ticket(bound);
        rejects(function () { foreign.admit(bound.child(Operation.HEALING)); }, "independent kernel rejects foreign lineage");
        check(isolated.currentTickCount() === 1, "kernel uses injected tick for admission count");
        clock++;
        check(isolated.currentTickCount() === 0, "injected tick advances budget window");
        rejects(function () { Kernel.of(new (cls("spi.ServerRuntimeBinding"))({combatnexus$serverRuntime: function () { return Java.loadClass("java.util.Optional").empty(); }})); }, "unbound lookup refuses creation");
    } finally { isolated.close(); foreign.close(); }
    rejects(function () { retained.requireActive(); }, "server close invalidates retained ticket");
    var closedBinding = new (cls("spi.ServerRuntimeBinding"))({combatnexus$serverRuntime: function () { return Java.loadClass("java.util.Optional").of(isolated); }});
    check(Kernel.existing(closedBinding).isEmpty(), "closed runtime is absent from lookup");
    rejects(function () { Kernel.of(closedBinding); }, "lookup cannot revive closed runtime");
    rejects(function () { new Kernel(new Session(), new Bool({getAsBoolean: function () { return false; }}), ticks); }, "kernel construction rejects wrong thread");
    return JSON.stringify({passed: checks.length, checks: checks});
})()
