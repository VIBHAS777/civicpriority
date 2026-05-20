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
    let time = 0;

    const resize = () => {
      W = canvas.width  = canvas.offsetWidth * (window.devicePixelRatio || 1);
      H = canvas.height = canvas.offsetHeight * (window.devicePixelRatio || 1);
      ctx.scale(window.devicePixelRatio || 1, window.devicePixelRatio || 1);
    };

    class Particle {
      constructor() { this.reset(true); }
      reset(init) {
        this.x  = Math.random() * (W / (window.devicePixelRatio || 1));
        this.y  = init ? Math.random() * (H / (window.devicePixelRatio || 1)) : (H / (window.devicePixelRatio || 1)) + 5;
        this.vx = (Math.random() - 0.5) * 0.35;
        this.vy = -(Math.random() * 0.35 + 0.08);
        this.r  = Math.random() * 2.5 + 0.5;
        this.hue   = 200 + Math.random() * 80;
        this.alpha = Math.random() * 0.55 + 0.15;
        this.pulseSpeed = Math.random() * 0.03 + 0.01;
        this.pulseOffset = Math.random() * Math.PI * 2;
      }
      update(t) {
        this.x += this.vx;
        this.y += this.vy;
        const rW = W / (window.devicePixelRatio || 1);
        const rH = H / (window.devicePixelRatio || 1);
        if (this.y < -5 || this.x < -5 || this.x > rW + 5) this.reset(false);
        this.currentAlpha = this.alpha * (0.7 + 0.3 * Math.sin(t * this.pulseSpeed + this.pulseOffset));
      }
      draw(t) {
        // Outer glow
        const glowR = this.r * 3;
        const glow = ctx.createRadialGradient(this.x, this.y, 0, this.x, this.y, glowR);
        glow.addColorStop(0, `hsla(${this.hue},85%,70%,${this.currentAlpha * 0.4})`);
        glow.addColorStop(1, `hsla(${this.hue},85%,70%,0)`);
        ctx.beginPath();
        ctx.arc(this.x, this.y, glowR, 0, Math.PI * 2);
        ctx.fillStyle = glow;
        ctx.fill();

        // Core dot
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.r, 0, Math.PI * 2);
        ctx.fillStyle = `hsla(${this.hue},85%,75%,${this.currentAlpha})`;
        ctx.fill();
      }
    }

    resize();
    window.addEventListener('resize', resize);
    const rW = () => W / (window.devicePixelRatio || 1);
    const rH = () => H / (window.devicePixelRatio || 1);
    const COUNT = 110;
    for (let i = 0; i < COUNT; i++) particles.push(new Particle());

    const MAX_DIST = 130;

    const tick = () => {
      time++;
      const w = rW(), h = rH();
      ctx.clearRect(0, 0, w, h);

      // Subtle energy wave across the bottom
      const waveY = h * 0.85;
      const waveGrad = ctx.createLinearGradient(0, waveY - 30, 0, waveY + 30);
      waveGrad.addColorStop(0, 'transparent');
      waveGrad.addColorStop(0.5, `rgba(71,118,230,${0.03 + 0.02 * Math.sin(time * 0.015)})`);
      waveGrad.addColorStop(1, 'transparent');
      ctx.fillStyle = waveGrad;
      ctx.beginPath();
      ctx.moveTo(0, waveY - 30);
      for (let x = 0; x <= w; x += 4) {
        const y = waveY + Math.sin(x * 0.008 + time * 0.02) * 15 + Math.sin(x * 0.015 - time * 0.01) * 8;
        ctx.lineTo(x, y);
      }
      ctx.lineTo(w, waveY + 30);
      ctx.lineTo(0, waveY + 30);
      ctx.closePath();
      ctx.fill();

      // Neural network connections with gradient
      for (let i = 0; i < particles.length; i++) {
        for (let j = i + 1; j < particles.length; j++) {
          const dx = particles[i].x - particles[j].x;
          const dy = particles[i].y - particles[j].y;
          const d  = Math.sqrt(dx*dx + dy*dy);
          if (d < MAX_DIST) {
            const op = (1 - d / MAX_DIST) * 0.2;
            const grad = ctx.createLinearGradient(
              particles[i].x, particles[i].y,
              particles[j].x, particles[j].y
            );
            grad.addColorStop(0, `rgba(71,118,230,${op})`);
            grad.addColorStop(0.5, `rgba(6,200,232,${op * 0.7})`);
            grad.addColorStop(1, `rgba(155,114,240,${op})`);
            ctx.beginPath();
            ctx.moveTo(particles[i].x, particles[i].y);
            ctx.lineTo(particles[j].x, particles[j].y);
            ctx.strokeStyle = grad;
            ctx.lineWidth = 0.6;
            ctx.stroke();
          }
        }
        particles[i].update(time);
        particles[i].draw(time);
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
