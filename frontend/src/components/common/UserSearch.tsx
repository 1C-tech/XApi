import { useState, type FormEvent } from 'react';
import { Search } from 'lucide-react';

interface UserSearchProps {
  onSearch: (userId: string) => void;
  loading: boolean;
}

export function UserSearch({ onSearch, loading }: UserSearchProps) {
  const [value, setValue] = useState('');

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    const trimmed = value.trim();
    if (trimmed && !loading) {
      onSearch(trimmed);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="relative">
      <div className="relative group">
        <Search
          size={16}
          className="
            absolute left-3 top-1/2 -translate-y-1/2
            text-text-secondary
            transition-colors duration-200
            group-focus-within:text-accent
          "
        />
        <input
          type="text"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          placeholder="输入用户 ID..."
          disabled={loading}
          className="
            w-full pl-9 pr-3 py-2 text-sm
            bg-tertiary border border-border
            rounded-lg
            text-text-primary placeholder:text-text-secondary/50
            outline-none
            transition-all duration-200
            focus:border-accent focus:ring-2 focus:ring-accent/20
            disabled:opacity-50 disabled:cursor-not-allowed
          "
        />
      </div>
    </form>
  );
}
