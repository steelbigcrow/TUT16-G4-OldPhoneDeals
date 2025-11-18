import React from 'react';

type FooterProps = {
  // reserved for future props
};

const Footer: React.FC<FooterProps> = () => {
  const year = new Date().getFullYear();

  return (
    <footer className="border-t bg-white">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-4 text-xs text-gray-500">
        <span>&copy; {year} Old Phone Deals</span>
        <span>Next.js frontend skeleton — TODO: add footer links</span>
      </div>
    </footer>
  );
};

export default Footer;