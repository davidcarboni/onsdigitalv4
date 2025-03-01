(function() {

	angular.module('onsApp')
		.config(['$locationProvider', '$httpProvider', '$logProvider', GeneralConfiguration])
		.run(['$http', 'DSCacheFactory', '$cacheFactory', '$log', RunConfiguration])

	function GeneralConfiguration($locationProvider, $httpProvider,$logProvider) {
		//Enable hashbang
		$locationProvider.html5Mode(false).hashPrefix('!')

		$logProvider.debugEnabled(false)
	}


	function RunConfiguration($http, DSCacheFactory, $cacheFactory, $log) {
		configureCache()

		function configureCache() {
			$log.info('Configuring data cache')

			var CACHE_NAME = 'dataCache'
				//Refer to: http://angular-data.pseudobry.com/documentation/guide/angular-cache/storage

			// Conditionally use Angular cache if local storage not supported
			//TODO: Create a caching design appropriate to 9.30 caching (e.g No caching between 9.30 - 9.31, expire all cache at 9.30) 
			var options = {
				maxAge: 1800000, // Items added to this cache expire after 30 minutes.
				cacheFlushInterval: 10800000, // This cache will clear itself every three hours.
				deleteOnExpire: 'aggressive', // Items will be deleted from this cache right when they expire.
				storageMode: 'localStorage' // This cache will sync itself with `localStorage`.
			};

			if (!hasLocalStorage()) {
				$log.warn('Local storage not supported, using angular cache')
				options.storageImpl = getAngularCache()
			}
			var dataCache = DSCacheFactory('dataCache', options);

			//Angular cache only uses session storage which is cleared itself when page is refreshed or browser closed
			function getAngularCache() {
				var cache = $cacheFactory('dataCache');

				function getItem(key) {
					// $log.debug('Angular cache getItem(): ', key)
					return cache.get(key)
				}

				function setItem(key, value) {
					// $log.debug('Angular cache setItem(): ', key, ',', value)
					return cache.put(key, value)
				}

				function removeItem(key) {
					// $log.debug('Angular cache removeItem(): ', key)
					cache.remove(key)
				}

				return localStoragePolyfill = {
					getItem: getItem,
					setItem: setItem,
					removeItem: removeItem
				}

			}

			function hasLocalStorage() {
				try { //Checking local storage fails if exlplicitly disable in some browsers (e.g. chrome),
					if (window.localStorage) {
						return true
					}
				} catch (err) {
					$log.warn('Failed detecting local storage, disabling local storage cache')
				}

				return false

			}

		}



	}

})()
