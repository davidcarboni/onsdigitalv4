angular.module('onsTemplates')


.controller('T6Ctrl', ['$scope',
    function($scope) {
        $scope.header = "Time Series";
        $scope.contentType = "timeseries";
        $scope.sidebar = true;
        $scope.sidebarUrl = "app/templates/t6/t6sidebar.html";
    }
])

.controller('GoogleChartCtrl', ['$scope', '$location', '$http',
    function($scope, $location, $http) {
        getTable("/googlechart.json", function(data) {
            $scope.chart = data;
            console.log($scope.chart);
        });
        function getTable(path, callback) {
            // console.log("Loading data at " + path);
            $http.get(path).success(callback);
        }
    }
]);
