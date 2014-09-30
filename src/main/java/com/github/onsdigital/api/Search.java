package com.github.onsdigital.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;

import org.apache.commons.lang3.StringUtils;

import com.github.davidcarboni.restolino.framework.Endpoint;
import com.github.onsdigital.util.ElasticSearchHTTPUtil;
import com.github.onsdigital.util.ONSQueryBuilder;
import com.github.onsdigital.util.SearchHTTPConnectionManager;
import com.github.onsdigital.util.ValidatorUtil;

@Endpoint
public class Search {
	final static String jsonMime = "application/json";
	final static String BONSAI_URL = System.getenv("BONSAI_URL");
	private final static float FIELD_BOOST = 2.0f;

	@GET
	public Object get(@Context HttpServletRequest request,
			@Context HttpServletResponse response) throws Exception {

		return search(extractQuery(request), extractPage(request),
				request.getParameter("type"));

	}

	private Object search(String query, int page, String type) throws Exception {
		SearchHTTPConnectionManager connectionManager = new SearchHTTPConnectionManager(
				BONSAI_URL);
		try {
			ONSQueryBuilder queryBuilder = new ONSQueryBuilder("ons")
					.setType(type).setPage(page).setSearchTerm(query)
					.setFields("title^" + FIELD_BOOST, "path");
			connectionManager.openConnection();

			ElasticSearchHTTPUtil searchUtil = new ElasticSearchHTTPUtil(
					connectionManager);

			return searchUtil.search(queryBuilder);
		} finally {
			connectionManager.closeConnection();
		}
	}

	private int extractPage(HttpServletRequest request) {
		String page = request.getParameter("page");
		if (StringUtils.isNotEmpty(page) && StringUtils.isNumeric(page)) {
			int pageNumber = Integer.parseInt(page);
			return pageNumber < 1 ? 1 : pageNumber;
		}
		return 1;
	}

	private String extractQuery(HttpServletRequest request) {
		String query = request.getParameter("q");

		if (StringUtils.isEmpty(query)) {
			// check to see if this is part of search's autocomplete
			query = request.getParameter("term");
			if (StringUtils.isEmpty(query)) {
				throw new RuntimeException("No search query provided");
			}
		}
		if (ValidatorUtil.isIllegalCharacter(query)) {
			throw new RuntimeException(
					"Search query can only contain alphanumeric characters");
		}

		return query;
	}
}
