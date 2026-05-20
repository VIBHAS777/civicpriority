import { useToast } from '../../context/ToastContext.jsx';
import s from './Toast.module.css';

export default function Toast() {
  const { toast } = useToast();
  if (!toast) return null;
  return (
    <div key={toast.key} className={`${s.toast} ${toast.type==='error'?s.error:''}`}>
      <span>{toast.type==='error'?'✗':'✓'}</span>{toast.message}
    </div>
  );
}
