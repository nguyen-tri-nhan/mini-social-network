class PageMeta {

  id = null;
  _title = null;
  onChange = null;

  constructor(onChange) {
    this.onChange = onChange;
  }

  get title() {
    return this._title;
  }

  set title(_title) {
    this._title = _title;
    this.onChange();
  }
}

export default PageMeta;
