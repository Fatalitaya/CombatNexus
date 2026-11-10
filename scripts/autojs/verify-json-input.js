(function () {
    var Json = Java.loadClass("io.github.fatalitaya.combatnexus.config.SettingsJson");
    var Reader = Java.loadClass("java.io.StringReader"), checks = [];
    function read(value) { return Json.read(new Reader(value)); }
    function check(value, label) { if (!value) throw new Error(label); checks.push(label); }
    var valid = read('{"number":-1.25e+2,"flag":true,"empty":null,"text":"\\u0041\\n","items":[false,0]}');
    check(valid.get("number").getAsDouble() === -125 && valid.get("flag").getAsBoolean(), "strict numbers and lowercase literals accepted");
    check(String(valid.get("text").getAsString()) === "A\n", "ASCII Unicode and control escapes decoded");
    ['{"x":TRUE}', '{"x":False}', '{"x":NULL}', '{"x":01}', '{"x":+1}', '{"x":.5}', '{"x":NaN}',
     '{"x":"\\v"}', '{"x":"\\uＦＦＦＦ"}', '{"x":"a\nb"}', '{"x":1,"x":2}', '{"x":1}{}', '{"x":1,}'].forEach(function (text) {
        var rejected = false;
        try { read(text); } catch (error) { rejected = true; }
        check(rejected, "invalid JSON rejected: " + text);
    });
    var rejected = false;
    try { Json.read(new Reader('{"x":123}'), 4, 20); } catch (error) { rejected = true; }
    check(rejected, "character budget enforced before parsing");
    rejected = false;
    try { Json.read(new Reader('{"x":[1,2]}'), 100, 2); } catch (error) { rejected = true; }
    check(rejected, "node budget enforced while parsing");
    return JSON.stringify({passed: checks.length, checks: checks});
})()
