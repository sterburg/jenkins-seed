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

String[] repositories = readFileFromWorkspace('docker.repositories').split('\n');
for(String repository : repositories)
{
	pipelineJob(configuration['JOB_ROOT'] + '/' + repository)
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
							url(sprintf(configuration['BITBUCKET_SCM_PATTERN'], repository))
							credentials(configuration['BITBUCKET_CREDENTIALS'])
						}
						branch('*/master')
						browser
						{
							stash(sprintf('%1$s/projects/AP/repos/%2$s/browse', configuration['BITBUCKET_URL'], repository))
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
				scriptPath('Jenkinsfile')
			}
		}
		description(
			descriptionTemplate.make(new MapWithDefaults([
				'jobType':'job',
			])).toString()
		)
		logRotator(-1, 30)
		quietPeriod(10)
		triggers
		{
			scm('')
		}
	}
}
