(function() {
    // Little Canvas things
    var canvas = document.querySelector("#canvas"),
        ctx = canvas.getContext('2d');

    // Set Canvas to be window size
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;

    // Configuration, Play with these
    var config = {
      particleNumber: 800,
      maxParticleSize: 10,
      maxSpeed: 40,
      colorVariation: 10
    };

    // Colors
    var colorPalette = {
        bg: {r:51, g:51, b:51},
        matter: [
            {r:39, g:150, b:47},
            {r:200, g:200, b:200},
            {r:70, g:199, b:145},
            {r:27, g:27, b:27}
        ]
    };

    // Some Variables hanging out
    var particles = [],
        centerX = canvas.width / 2,
        centerY = canvas.height / 2,
        drawBg,

    // Draws the background for the canvas, because space
    drawBg = function (ctx, color) {
        ctx.fillStyle = "rgb(" + color.r + "," + color.g + "," + color.b + ")";
        ctx.fillRect(0,0,canvas.width,canvas.height);
    };

    // Particle Constructor
    var Particle = function (x, y) {
        // X Coordinate
        this.x = x || Math.round(Math.random() * canvas.width);
        // Y Coordinate
        this.y = y || Math.round(Math.random() * canvas.height);
        // Radius of the space dust
        this.r = Math.ceil(Math.random() * config.maxParticleSize);
        // Color of the rock, given some randomness
        this.c = colorVariation(colorPalette.matter[Math.floor(Math.random() * colorPalette.matter.length)],true );
        // Speed of which the rock travels
        this.s = Math.pow(Math.ceil(Math.random() * config.maxSpeed), .7);
        // Direction the Rock flies
        this.d = Math.round(Math.random() * 360);
    };

    // Provides some nice color variation
    // Accepts an rgba object
    // returns a modified rgba object or a rgba string if true is passed in for argument 2
    var colorVariation = function (color, returnString) {
        var r,g,b,a, variation;
        r = Math.round(((Math.random() * config.colorVariation) - (config.colorVariation/2)) + color.r);
        g = Math.round(((Math.random() * config.colorVariation) - (config.colorVariation/2)) + color.g);
        b = Math.round(((Math.random() * config.colorVariation) - (config.colorVariation/2)) + color.b);
        a = Math.random() + .5;
        if (returnString) {
            return "rgba(" + r + "," + g + "," + b + "," + a + ")";
        } else {
            return {r,g,b,a};
        }
    };

    // Used to find the rocks next point in space, accounting for speed and direction
    var updateParticleModel = function (p) {
        var a = 180 - (p.d + 90); // find the 3rd angle
        p.d > 0 && p.d < 180 ? p.x += p.s * Math.sin(p.d) / Math.sin(p.s) : p.x -= p.s * Math.sin(p.d) / Math.sin(p.s);
        p.d > 90 && p.d < 270 ? p.y += p.s * Math.sin(a) / Math.sin(p.s) : p.y -= p.s * Math.sin(a) / Math.sin(p.s);
        return p;
    };

    // Just the function that physically draws the particles
    // Physically? sure why not, physically.
    var drawParticle = function (x, y, r, c) {
        ctx.beginPath();
        ctx.fillStyle = c;
        ctx.arc(x, y, r, 0, 2*Math.PI, false);
        ctx.fill();
        ctx.closePath();
    };

    // Remove particles that aren't on the canvas
    var cleanUpArray = function () {
        particles = particles.filter((p) => { 
          return (p.x > -100 && p.y > -100); 
        });
    };


    var initParticles = function (numParticles, x, y) {
        for (let i = 0; i < numParticles; i++) {
            particles.push(new Particle(x, y));
        }
        particles.forEach((p) => {
            drawParticle(p.x, p.y, p.r, p.c);
        });
    };

    // That thing
    var requestAnimFrame = (function() {
      return window.requestAnimationFrame ||
         window.webkitRequestAnimationFrame ||
         window.mozRequestAnimationFrame ||
         function(callback) {
            window.setTimeout(callback, 1000 / 60);
         };
    })();


    // Our Frame function
    var frame = function () {
      // Draw background first
      drawBg(ctx, colorPalette.bg);
      // Update Particle models to new position
      particles.map((p) => {
        return updateParticleModel(p);
      });
      // Draw em'
      particles.forEach((p) => {
          drawParticle(p.x, p.y, p.r, p.c);
      });
      // Play the same song? Ok!
      requestAnimFrame(frame);
    };

    /*
        // Click listener
        document.body.addEventListener("click", function (event) {
            var x = event.clientX,
                y = event.clientY;
            cleanUpArray();
            initParticles(config.particleNumber, x, y);
        });
    */

    // First Frame
    frame();

    // First particle explosion
    initParticles(config.particleNumber);

    window.particles = {
        render: function() {
            cleanUpArray();

            const maxHeight = window.innerHeight;
            const maxWidth = window.innerWidth;

            const x = ((maxWidth / 2) - 128);
            const y = ((maxHeight / 2) - 128);

            initParticles(800, (x + 64), (y + 64));

            window.setTimeout(function() {
                initParticles(800, (x + 256), (y + 64));
            }, 128);

            window.setTimeout(function() {
                initParticles(800, (x + 128), (y + 192));
            }, 512);

            window.setTimeout(function() {
                initParticles(800, (x + 64), (y + 256));
            }, 768);

            window.setTimeout(function() {
                initParticles(800, (x + 128), (y + 128));
            }, 1536);

            window.setTimeout(function() {
                const thankYouDiv = document.querySelector("#thank-you");
                thankYouDiv.classList.toggle("visible", true);
            }, 1536);

            window.setTimeout(function() {
                const canvas = document.querySelector("#canvas");
                const thankYouDiv = document.querySelector("#thank-you");
                $(canvas).fadeOut(750, function() {
                    canvas.classList.toggle("visible", false);
                    $(canvas).attr("style", null); // Allow the "visible" class to handle visibility, not inline styles.
                });
                $(thankYouDiv).fadeOut(750, function() {
                    thankYouDiv.classList.toggle("visible", false);
                    $(thankYouDiv).attr("style", null); // Allow the "visible" class to handle visibility, not inline styles.
                });
            }, 7000);

            window.setTimeout(cleanUpArray, 5000);
        }
    };
})();
