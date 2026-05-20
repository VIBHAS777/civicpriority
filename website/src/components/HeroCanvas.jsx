import { useRef, useEffect } from 'react';
import s from './HeroCanvas.module.css';

export default function HeroCanvas() {
  const canvasRef = useRef(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    let animId;
    let particles = [];
    let W = 0, H = 0;

    const resize = () => {
      W = canvas.width  = canvas.offsetWidth;
      H = canvas.height = canvas.offsetHeight;
    };

    class Particle {
      constructor() { this.reset(true); }
      reset(init) {
        this.x  = Math.random() * W;
        this.y  = init ? Math.random() * H : H + 5;
        this.vx = (Math.random() - 0.5) * 0.4;
        this.vy = -(Math.random() * 0.4 + 0.1);
        this.r  = Math.random() * 2 + 0.5;
        this.hue   = 200 + Math.random() * 80; // blue-purple
        this.alpha = Math.random() * 0.55 + 0.15;
        this.life  = 1;
      }
      update() {
        this.x += this.vx;
        this.y += this.vy;
        if (this.y < -5 || this.x < -5 || this.x > W + 5) this.reset(false);
      }
      draw() {
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.r, 0, Math.PI * 2);
        ctx.fillStyle = `hsla(${this.hue},85%,70%,${this.alpha})`;
        ctx.fill();
      }
    }

    resize();
    window.addEventListener('resize', resize);
    for (let i = 0; i < 90; i++) particles.push(new Particle());

    const MAX_DIST = 110;
    const tick = () => {
      ctx.clearRect(0, 0, W, H);
      // connections
      for (let i = 0; i < particles.length; i++) {
        for (let j = i + 1; j < particles.length; j++) {
          const dx = particles[i].x - particles[j].x;
          const dy = particles[i].y - particles[j].y;
          const d  = Math.sqrt(dx*dx + dy*dy);
          if (d < MAX_DIST) {
            const op = (1 - d / MAX_DIST) * 0.18;
            ctx.beginPath();
            ctx.moveTo(particles[i].x, particles[i].y);
            ctx.lineTo(particles[j].x, particles[j].y);
            ctx.strokeStyle = `rgba(71,118,230,${op})`;
            ctx.lineWidth = 0.6;
            ctx.stroke();
          }
        }
        particles[i].update();
        particles[i].draw();
      }
      animId = requestAnimationFrame(tick);
    };
    tick();

    return () => {
      cancelAnimationFrame(animId);
      window.removeEventListener('resize', resize);
    };
  }, []);

  return <canvas ref={canvasRef} className={s.canvas} />;
}
