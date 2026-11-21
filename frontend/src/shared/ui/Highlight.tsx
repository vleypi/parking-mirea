interface IHighlightProps {
  text: string;
  query: string;
}

export const Highlight = ({ text, query }: IHighlightProps) => {
  const start = query ? text.toLowerCase().indexOf(query.toLowerCase()) : -1;
  if (start < 0) return text;

  const end = start + query.length;

  return (
    <>
      {text.slice(0, start)}
      <mark className="rounded-sm bg-lime/50 text-inherit">{text.slice(start, end)}</mark>
      {text.slice(end)}
    </>
  );
};
