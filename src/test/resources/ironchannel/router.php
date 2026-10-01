<?php
declare(strict_types=1);

// Isolated verifier only: no Feature API config, database, or business endpoints.
require getenv('FEATURE_API_SOURCE').'/autoload.php';

\TRP\FeatureApi\Core\Authorization::register_key('abc123', 'secret-key');
try {
	$endpoint = substr($_SERVER['REQUEST_URI'], strlen('/feature-api/'));
	\TRP\FeatureApi\Core\Authorization::verify_header($endpoint);
	header('Content-Type: application/json');
	echo '{"authenticated":true}';
} catch (\Throwable $error) {
	http_response_code(in_array($error->getCode(), [400, 401, 403], true) ? $error->getCode() : 500);
	echo '{"authenticated":false}';
}
