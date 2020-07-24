declare var ReactDOMServer;
declare function __calljava(args: string): string;

interface State {
}

interface Props {
	title: string;
	text: string;
}

class ContentItem extends React.Component<Props,State> {
	render() {
		const {title, text} = this.props as any;
		return (
			<div>
				<h1>{title}</h1>
				<p>{text}</p>
			</div>
		);
	}
}

function renderHTML() {
	debugger;
	const html = ReactDOMServer.renderToStaticMarkup(
		<body>
			<ContentItem title="Example title" text="Example text"/>
		</body>
	);
	return html;
}
