var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var ContentItem = /** @class */ (function (_super) {
    __extends(ContentItem, _super);
    function ContentItem() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    ContentItem.prototype.render = function () {
        var _a = this.props, title = _a.title, text = _a.text;
        return (React.createElement("div", null,
            React.createElement("h1", null, title),
            React.createElement("p", null, text)));
    };
    return ContentItem;
}(React.Component));
function renderHTML() {
    debugger;
    var html = ReactDOMServer.renderToStaticMarkup(React.createElement("body", null,
        React.createElement(ContentItem, { title: "Example title", text: "Example text" })));
    return html;
}
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoiYnVpbGQuanMiLCJzb3VyY2VSb290IjoiIiwic291cmNlcyI6WyJtYWluLnRzeCJdLCJuYW1lcyI6W10sIm1hcHBpbmdzIjoiOzs7Ozs7Ozs7Ozs7O0FBV0E7SUFBMEIsK0JBQTRCO0lBQXREOztJQVVBLENBQUM7SUFUQSw0QkFBTSxHQUFOO1FBQ08sSUFBQSxlQUFpQyxFQUFoQyxnQkFBSyxFQUFFLGNBQXlCLENBQUM7UUFDeEMsT0FBTyxDQUNOO1lBQ0MsZ0NBQUssS0FBSyxDQUFNO1lBQ2hCLCtCQUFJLElBQUksQ0FBSyxDQUNSLENBQ04sQ0FBQztJQUNILENBQUM7SUFDRixrQkFBQztBQUFELENBQUMsQUFWRCxDQUEwQixLQUFLLENBQUMsU0FBUyxHQVV4QztBQUVELFNBQVMsVUFBVTtJQUNsQixRQUFRLENBQUM7SUFDVCxJQUFNLElBQUksR0FBRyxjQUFjLENBQUMsb0JBQW9CLENBQy9DO1FBQ0Msb0JBQUMsV0FBVyxJQUFDLEtBQUssRUFBQyxlQUFlLEVBQUMsSUFBSSxFQUFDLGNBQWMsR0FBRSxDQUNsRCxDQUNQLENBQUM7SUFDRixPQUFPLElBQUksQ0FBQztBQUNiLENBQUMifQ==