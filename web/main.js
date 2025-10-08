"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var rawSrc = 'assets/raw_image.jpg';
var edgesSrc = 'assets/edge_image.jpg';
if (!document.getElementById('frame') || !document.getElementById('fps') || !document.getElementById('res') || !document.getElementById('toggleBtn')) {
    console.error('Missing DOM elements (frame/fps/res/toggleBtn)');
}
else {
    var frame_1 = document.getElementById('frame');
    var fpsEl_1 = document.getElementById('fps');
    var resEl = document.getElementById('res');
    var btn_1 = document.getElementById('toggleBtn');
    var mode_1 = 'raw';
    var simulatedFps_1 = 30;
    var updateFrame_1 = function () {
        frame_1.src = mode_1 === 'raw' ? rawSrc : edgesSrc;
    };
    var toggleMode = function () {
        mode_1 = mode_1 === 'raw' ? 'edges' : 'raw';
        btn_1.textContent = mode_1 === 'raw' ? 'Mode: Raw' : 'Mode: Edges';
        updateFrame_1();
    };
    btn_1.addEventListener('click', toggleMode);
    // simulate FPS updates every second
    var tick = function () {
        simulatedFps_1 = 28 + Math.floor(Math.random() * 6);
        fpsEl_1.textContent = simulatedFps_1.toString();
    };
    // static resolution per requirement
    resEl.textContent = '1920x1080';
    updateFrame_1();
    setInterval(tick, 1000);
}
