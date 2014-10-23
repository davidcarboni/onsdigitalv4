//Main ons applucation code
'use strict';

(function() {

    // Components are to be injected to onsComponents module
    var onsComponents = angular.module('onsComponents', [])

    //Filters and other helpers are to be injected to onsHelpers module
    var onsHelpers = angular.module('onsHelpers', [])

    //Template related components are to be registered to onsTemplates module
    var onsTemplates = angular.module('onsTemplates', [])

    /* App Module */
    var onsApp = angular.module('onsApp', [
        'ngRoute',
        'onsHelpers',
        'onsComponents',
        'onsTemplates'
    ])

    onsApp
        .config(['$routeProvider', '$locationProvider',
            function($routeProvider, $locationProvider) {
                $routeProvider.

                when('/release', {
                    templateUrl: 'app/templates/release/release.html'
                }).
                when('/collection', {
                    templateUrl: 'app/templates/collection/collection.html',
                    controller: "CollectionCtrl"
                }).
                when('/economy/inflationandpriceindices/bulletin', {
                    templateUrl: 'app/templates/bulletin/bulletin.html',
                    controller: 'BulletinCtrl'
                }).
                when('/bulletin', {
                    redirectTo: '/economy/inflationandpriceindices/bulletin'
                }).
                when(':*\/bulletins', {
                    templateUrl: 'app/templates/bulletin/bulletin.html',
                    controller: 'BulletinCtrl'
                }).
                when('/methodology', {
                    templateUrl: 'app/templates/methodology/methodology.html',
                    controller: 'MethodologyCtrl'
                }).
                when('/article', {
                    templateUrl: 'app/templates/article/article.html',
                    controller: 'ArticleCtrl'
                }).
                when('/search', {
                    templateUrl: 'app/templates/search-results/search-results.html',
                    controller: 'SearchCtrl'
                }).
                when('/', {
                    templateUrl: 'app/templates/taxonomy/taxonomy.html',
                    controller: 'TaxonomyController',
                    contollerAs: 'ctrl'
                }).
                otherwise({
                    templateUrl: 'app/templates/taxonomy/taxonomy.html',
                    controller: 'TaxonomyController',
                    contollerAs: 'ctrl',
                    resolve: {
                        // https://stackoverflow.com/questions/15975646/angularjs-routing-to-empty-route-doesnt-work-in-ie7
                        // Here we ensure that our route has the document fragment (#), or more specifically that it has #/ at a minimum.
                        // If accessing the base URL without a trailing '/' in IE7 it will execute the otherwise route instead of the signin
                        // page, so this check will ensure that '#/' exists and if not redirect accordingly which fixes the URL.
                        redirectCheck: ['$location',
                            function($location) {
                                var absoluteLocation = $location.absUrl();
                                if (absoluteLocation.indexOf('#/') === -1) {
                                    $location.path('/');
                                }
                            }
                        ]
                    }
                })

            }
        ])

    onsApp
        .controller('MainCtrl', ['$scope', '$log', '$http', '$location', 'PathService',
            function($scope, $log, $http, $location, PathService) {
                var ctrl = this

                loadNavigation()

                function loadNavigation() {
                    $http.get("/navigation").success(function(data) {
                        $log.debug('Main Ctrl: Loading navigation data')
                        $scope.navigation = data
                        $log.debug('Main Ctrl: navigation data loaded successfully ', data)
                    })
                }

                $scope.getPath =function()  {
                    return PathService.getPath()
                }

                $scope.getPage = function() {
                  return PathService.getPage()
                }

                $scope.getUrlParam=function(paramName) {
                  var params = $location.search()
                  return params[paramName]
                }


            }
        ])

})()