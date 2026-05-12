document.addEventListener('DOMContentLoaded', () => {
  // Strategy Segmented Control
  const segments = document.querySelectorAll('.segment');
  segments.forEach(segment => {
    segment.addEventListener('click', () => {
      segments.forEach(s => s.classList.remove('active'));
      segment.classList.add('active');
    });
  });

  // KPI Sliders Auto-balancing
  const w1Slider = document.querySelectorAll('.slider')[0];
  const w2Slider = document.querySelectorAll('.slider')[1];
  
  const w1Value = document.querySelectorAll('.slider-value')[0];
  const w2Value = document.querySelectorAll('.slider-value')[1];

  w1Slider.addEventListener('input', (e) => {
    const val = parseInt(e.target.value, 10);
    w2Slider.value = 100 - val;
    
    w1Value.textContent = (val / 100).toFixed(1);
    w2Value.textContent = ((100 - val) / 100).toFixed(1);
  });

  w2Slider.addEventListener('input', (e) => {
    const val = parseInt(e.target.value, 10);
    w1Slider.value = 100 - val;
    
    w2Value.textContent = (val / 100).toFixed(1);
    w1Value.textContent = ((100 - val) / 100).toFixed(1);
  });

  // Anomaly Simulator Interaction
  const simulateBtn = document.querySelector('.btn-danger');
  const terminal = document.querySelector('.terminal');
  
  simulateBtn.addEventListener('click', () => {
    const t = document.getElementById('detection-time').value;
    
    const now = new Date();
    const timeStr = now.toTimeString().split(' ')[0];
    
    const newLog = document.createElement('div');
    newLog.innerHTML = `<span class="log-time">[${timeStr}]</span> <span class="log-sys">USER_CMD:</span> Simulated Anomaly injected at t=${t}.`;
    
    terminal.appendChild(newLog);
    terminal.scrollTop = terminal.scrollHeight;
    
    // Animate Gantt visually to show interaction
    const blocks = document.querySelectorAll('.gantt-block');
    blocks.forEach(block => {
      block.style.transform = 'scale(0.98)';
      setTimeout(() => {
        block.style.transform = 'scale(1)';
      }, 150);
    });
  });
});
