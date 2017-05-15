<pagination>
  <p>{currentPageNumber} / {allPageCount}</p>
  <ul>
    <li if={isFirst}><a href={prevPageParam} onclick={movePage}>prev</a></li>
    <li each={prevPages} class="page"><a href={pageParam} onclick={movePage}>{pageNum}</a></li>
    <li class="current">{currentPageNumber}</li>
    <li each={nextPages} class="page"><a href={pageParam} onclick={movePage}>{pageNum}</a></li>
    <li if={isEnd}><a href={nextPageParam} onclick={movePage}>next</a></li>
  </ul>

  <style scoped>
    /*{
      display: block;
      text-align: center;
      margin-top: 30px;
    }*/
    p {
      display: block;
      font-size: 12px;
      color: #888;
    }
    ul {
      margin-top: 3px;
    }
    li {
      display: inline-block;
      margin: 4px;
    }
    a,
    li a {
      color: #666;
    }
    .page a {
      padding: 2px 6px;
      border: 1px #825 solid;
      background: #fff;
    }
    .current {
      padding: 3px 7px;
      background:#825;
      color: #fff;
    }
    a:hover {
      text-decoration: none;
      opacity: 0.8;
    }
  </style>

  <script>
    var RC = window.RC || {};
    var obs = window.observable || {};
    var self = this;
    var range = 3;

    movePage = function(e) {
      e.preventDefault();
      var href= e.target.pathname + e.target.search
      obs.trigger(RC.EVENT.route.change, href);
    };

    this.currentPageNumber = 1;
    this.allPageCount = 1;
    this.prevPages = [];
    this.nextPages = [];
    this.isFirst = false;
    this.isEnd = false;

    var mappingPagenation = function(data) {
      self.prevPages = [];
      self.nextPages = [];
      self.currentPageNumber = data.currentPageNumber;
      self.allPageCount = data.allPageCount;
      var queryParams = window.helper.mappingQueryParams();

      // set prevPages
      var prevStart = self.currentPageNumber - range;
      prevStart = (prevStart <= 1) ? 1 : prevStart;
      for (var i = prevStart; i < self.currentPageNumber; i++) {
        var q = window.helper.updateOrInsertQueryParams(queryParams, "page", i);
        self.prevPages.push({pageNum: i, pageParam: window.helper.joinQueryParams(q)});
      }

      // set nextPages
      var nextEnd = self.currentPageNumber + range;
      nextEnd = (nextEnd >= self.allPageCount) ? self.allPageCount : nextEnd;
      for (var i = self.currentPageNumber + 1; i <= nextEnd; i++) {
        var q = window.helper.updateOrInsertQueryParams(queryParams, "page", i);
        self.nextPages.push({pageNum: i, pageParam: window.helper.joinQueryParams(q)});
      }

      // is First or End
      self.isFirst = self.currentPageNumber != 1;
      var prevQuery = window.helper.updateOrInsertQueryParams(queryParams, "page", self.currentPageNumber - 1);
      self.prevPageParam = window.helper.joinQueryParams(prevQuery);
      var nextQuery = window.helper.updateOrInsertQueryParams(queryParams, "page", self.currentPageNumber + 1);
      self.nextPageParam = window.helper.joinQueryParams(nextQuery);
      self.isEnd = self.currentPageNumber != self.allPageCount;

      self.update();
    }

    obs.on(RC.EVENT.pagenation.set, function(data) {
      mappingPagenation(data);
    });
  </script>
</pagination>
