'use strict';

(function() {

	angular.module('onsSparkline', ['highcharts-ng'])
		.directive('onsSparkline', ['$log',
			SparkLine
		])


	function SparkLine($log) {
		return {
			restrict: 'E',
			scope: {
				chartData: '=',
				width: '=',
				height: '='
			},
			controller: SparkLineController,
			controllerAs: 'sparkline',
			templateUrl: 'app/components/sparkline/sparkline.html'
		}

		function SparkLineController($scope) {
			var sparkline = this
			sparkline.visible = false
			// sparkline.padding=$scope.padding
			sparkline.chartConfig = getSparkline($scope.width, $scope.height)
			watchData()
			// watchHeight()
			// watchWidth()

			function watchData() {
				$scope.$watch('chartData', function() {
					if ($scope.chartData) {
						buildChart(sparkline.chartConfig, $scope.chartData, $scope.headline)
						sparkline.visible = true
					} else {
						sparkline.visible = false
					}
				})
			}

			function watchHeight() {
				$scope.$watch('height', function() {
					console.log("height" + $scope.height)
					sparkline.chartConfig.options.chart.height = $scope.height
				})
			}

			function watchWidth() {
				$scope.$watch('width', function() {
					console.log("width" + $scope.width)
					sparkline.chartConfig.options.chart.width = $scope.width
				})
			}

			function buildChart(chartConfig, timeseries, isHeadline) {
			
				prepareData()

				function prepareData() {
					chartConfig.series[0].data = formatData(timeseries).values
					chartConfig.options.xAxis.tickInterval = tickInterval(timeseries.length);

					function tickInterval(length) {
						if (length <= 20) {
							return 1;chartConfig
						} else if (length <= 80) {
							return 4;
						} else if (length <= 240) {
							return 12;
						} else if (length <= 480) {
							return 48;
						} else if (length <= 960) {
							return 96;
						} else {
							return 192;
						}
					}

					//Format data into high charts compatible format
					function formatData(timeseries) {
						var data = {
							values: []
						}

						var current
						var i

						for (i = 0; i < timeseries.length; i++) {
							current = timeseries[i]
							data.values.push(enrichData(current))
						}
						return data
					}

					function enrichData(timeseries) {
						timeseries.y = +timeseries.value //Cast to number
						timeseries.name = timeseries.date //Appears on x axis
						return timeseries
					}
				}
			}
		}
	}



	function getSparkline(width, height) {
		// var widthValue
		// var heightValue
		// if (isHeadline) {
		// 	widthValue = 170
		// 	heightValue = 100
		// } else {
		// 	widthValue = 140
		// 	heightValue = 50
		// }

		var data = {
			options: {
				chart: {
					backgroundColor: null,
					borderWidth: 0,
					type: 'area',
					margin: [0, 0, 0, 0],
					width: width,
					height: height ,
					style: {
						overflow: 'visible'
					},
					skipClone: true
				},
				title: {
					text: ''
				},
				subtitle: {
					text: '',
					y: 110
				},
				credits: {
					enabled: false
				},
				xAxis: {
					categories: [],
					labels: {
						formatter: function() {
							if (this.isFirst) {
								return this.value
							}
							if (this.isLast) {
								return this.value
							}
						},
						step: 1
					},
					tickLength: 0
				},
				yAxis: {
					endOnTick: false,
					startOnTick: false,
					labels: {
						enabled: false
					},
					title: {
						text: null
					},
					tickPositions: [0]
				},
				legend: {
					enabled: false
				},
				tooltip: {
					enabled: false
				},
				plotOptions: {
					series: {
						animation: false,
						lineWidth: 1,
						shadow: false,
						states: {
							hover: {
								lineWidth: 1
							}
						},
						marker: {
							radius: 1,
							states: {
								hover: {
									radius: 2
								}
							}
						},
						fillOpacity: 0.25,
						enableMouseTracking: false,
					},
					column: {
						negativeColor: '#910000',
						borderColor: 'silver'
					}
				},
				exporting: {
					enabled: false
				}
			},
			series: [{
				name: "",
				data: [],
				marker: {
					symbol: "circle",
					states: {
						hover: {
							fillColor: '#007dc3',
							radiusPlus: 0,
							lineWidthPlus: 0
						}
					}
				},
				dashStyle: 'Solid',
			}]
		};
		return data;
	}

})()