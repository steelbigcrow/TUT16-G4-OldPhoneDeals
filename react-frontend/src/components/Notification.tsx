type NotificationProps = {
  message?: string;
  type?: 'success' | 'error' | 'info';
};

const Notification = ({ message, type = 'info' }: NotificationProps) => {
  if (!message) return null;

  return (
    <div className={`notice ${type}`}>
      {message}
    </div>
  );
};

export default Notification;
