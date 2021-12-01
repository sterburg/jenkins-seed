import groovy.text.SimpleTemplateEngine;

// Assign defaults
Map configuration = new HashMap();
configuration['BITBUCKET_SCM_PATTERN'] = 'ssh://git@bitbucket:7999/ap/%1$s.git';
configuration['BITBUCKET_URL'] = 'https://bitbucket.swift.com';
configuration['BITBUCKET_CREDENTIALS'] = 'd25c0a64-8d3d-44a7-ac3f-8b6b30a2b2e6';
configuration['JOB_ROOT'] = 'ap';

// Load the configuration from the binding (overriding defaults)
configuration.putAll(binding.variables);

def descriptionTemplate = (new SimpleTemplateEngine())
							.createTemplate(readFileFromWorkspace('description.template'))

class MapWithDefaults {
	@Delegate private final Map map
	MapWithDefaults(Map map) { this.map = map; }
	boolean containsKey(Object key) { return true; }
	Object get(Object key) { return map.getOrDefault(key, ''); }
}

String[] environments = readFileFromWorkspace('bootstrap.environments').split('\n');
for(String environment : environments)
{
	if(environment.startsWith("#") || environment.startsWith("//")) continue;

	segments = environment.split(":")
	pipelineJob(configuration['JOB_ROOT'] + '/bootstrap-' + segments[0])
	{
		definition
		{
			cpsScm
			{
				lightweight(true)
				scm
				{
					git
					{
						remote
						{
							url(sprintf('ssh://git@%1$s:7999/ap/bootstrap.git', '10.64.73.65'))
							credentials(configuration['BITBUCKET_CREDENTIALS'])
						}
						branch('*/master')
						browser
						{
							stash(configuration['BITBUCKET_URL'] + '/projects/AP/repos/bootstrap/browse')
						}
						extensions
						{
							cleanBeforeCheckout()
							cloneOptions
							{
								noTags(true)
								shallow(true)
							}
						}
					}
				}
				scriptPath('Jenkinsfile.' + segments[0])
			}
		}
		description(
			descriptionTemplate.make(new MapWithDefaults([
				'jobType':'job',
				'preInfo': 'Pipeline to bootstrap the deployment of APX in the ' + segments[0].toUpperCase() + ' environment.<br />'
			])).toString()
		)
		logRotator(-1, 30)
		quietPeriod(10)
		triggers
		{
			// All existing environments will be deployed once a day, unless otherwise triggered.
			if(segments.length < 2)
			{
				cron('H 5 * * *')
			}
			scm('')
			if(segments.length > 1)
			{
				upstream(configuration['JOB_ROOT'] + '/bootstrap-' + segments[1], 'SUCCESS')
			}
		}
	}
}
