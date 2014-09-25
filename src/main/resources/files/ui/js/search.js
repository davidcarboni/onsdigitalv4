/**
* ons search
* Jquery based search plugin to search and generated markup for results
*
*/


$.extend({
	search: function(options) {
		var onssearch = $.extend({
			query : '',
			resultsHolder : '',
			paginatorHolder : '',
			searchResultLegeng : '' ,
			page: 0,
			onSearchStart: function () {
				// Callback triggered before searching
			},
			onSearchComplete: function() {
			// Callback triggered when search is complete and markup generated
			}
		}, options || {});


		onssearch.onSearchStart();

		// attach search widget to document body
		$('body').data('onssearch', onssearch);
		
		if (!onssearch.query) {
			createDummyresults();
			return onssearch;
		}

		doSearch();

		return onssearch;
	} 			
});


function loadPage(pageNumber) {
	var onssearch = $('body').data('onssearch');
	onssearch.page = pageNumber || onssearch.page;
	doSearch();
}


function doSearch() {
		
		var onssearch = $('body').data('onssearch');
		
		$.getJSON("search?q=" + onssearch.query + "&page=" + onssearch.page,  function(data) {
			  	console.log( "Search successful for query " + 	onssearch.query );
				console.log(data);
				onssearch.data = data;  
			  	buildResultList();
			  	buildPaginator();
			  	onssearch.onSearchComplete();
			  }).fail(function() {
			    console.log( "Failed searching for query " + keyword );
		});
}


function buildResultList() {
	
	var onssearch = $('body').data('onssearch');
	var resultsContainer = $('#' + onssearch.resultsHolder);
	if (!resultsContainer) {
		return;
	};

	clearDiv(resultsContainer);	
	
	var results = onssearch.data.results;
	var dl = $("<dl/>"); 
	resultsContainer.append(dl);
		
	for (i = 0; i < results.length; i++) {
			var dt =  $("<dt class=''/>");
			dt.append("<a href='#' >" +  results[i].title + "</a>");
			dt.append("<span class='lozenge' href='#'>" + results[i].type +  "</span>"  );
			var releaseDate =  $("<dd class='microcopy'>Released ...." + "</dd>");
			var content = $("<dd>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque sit amet diam vel est congue eleifend non eget sem. Nam enim ipsum, vestibulum sed facilisis ut, commodo et nulla. Aliquam erat volutpat. Sed ut aliquet nunc. Ut mollis, arcu non vehicula gravida, lectus magna auctor quam, in rhoncus nibh nisl nec odio. Nam suscipit, dui in maximus porttitor, velit nulla rutrum leo, sit amet porttitor tellus dolor sit amet est. Quisque nulla augue, gravida sit amet mauris vel, congue aliquam urna. Quisque consectetur commodo mi eu pulvinar. Nam egestas magna sem, non sagittis massa vehicula non. Cras ultricies eleifend scelerisque. Pellentesque nunc tellus, commodo eget est nec, molestie elementum nulla. Donec mattis sed nisl in tincidunt. Mauris</dd>");

   	 	dl.append(dt);
   	 	dl.append(releaseDate);
   	 	dl.append(content);
	}	
}

function buildPaginator() {

	var onssearch = $('body').data('onssearch');
	var paginatorContainer = $('#' + onssearch.paginatorHolder);
	if (!paginatorContainer) {
		return;
	};	

    $(paginatorContainer).pagination({
        items: onssearch.data.numberOfResults,
        itemsOnPage: 10,
        displayedPages: 10,
        currentPage : onssearch.page,
        edges: 0,
        cssStyle: "pagination",
        hideIfSinglePage : true,
        hideNextOnLastPage : true,
        hidePrevOnFirstPage : true,
		onPageClick: function (pageNumber) {
			loadPage(pageNumber);		
		}
    });
}

function clearDiv(div) {
//Clear div content
div.html('');
}

function  createDummyresults() {
	
	var onssearch = $('body').data('onssearch');

	var data={
		took:10,
		numberOfResults:1004,
		results:new Array()
	}
	
	for (i=0; i<10;i++) {
		data.results[i] = { title: "Title" + i, tags: "Tag" + i, type: "dummy" } 	
	}	

	onssearch.data = data;
	buildResultList();
	buildPaginator();
	onssearch.onSearchComplete();
}