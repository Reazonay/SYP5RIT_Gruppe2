export default function TokenInput({
  value,
  setValue,
  label,
}: {
  value: string;
  setValue: (s: string) => void;
  label: string;
}) {
  return (
    <label>
      {label}
      <input
        type="password"
        autoComplete="off"
        value={value}
        onChange={(e) => {
          let v = e.target.value;
          const match = v.match(/(?:guest|group)=([a-f0-9]{64})/);
          setValue(match ? match[1] : v);
        }}
        placeholder="Zugriffscode oder Link aus der E-Mail"
      />
    </label>
  );
}
