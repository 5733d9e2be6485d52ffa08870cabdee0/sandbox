import requests
import argparse
import threading
import time
import json 
import random 
import uuid

global url
global keycloak
global username
global password
global bearer_token
global bad_request_rate
global match_filter_rate

bridges = []
processors = []

def create_bridge():
    while True:
        if (random.random() < bad_request_rate and bridges): # with probability `bad_request_rate` create a bad request
            print('[bridge] Creating a bad request')
            body = {'name': bridges[0]}
        else: #create a valid request
            print('[bridge] Creating a valid request')
            body = {'name': str(uuid.uuid4())}

        response = requests.post(url + '/api/v1/bridges', data=json.dumps(body), 
        headers={'Content-type': 'application/json', 'Authorization': 'Bearer {0}'.format(get_bearer_token())})
        if (response.status_code == 401):
            authenticate()
            continue
        elif(response.status_code == 202):
            bridges.append(json.loads(response.text)['id'])
        print('[bridge] response status code: {0}'.format(response.status_code))
        print('[bridge] sleeping for 30 seconds')
        time.sleep(30)

def create_processor():
    while True:
        if (len(bridges) == 0):
            continue

        if (random.random() < bad_request_rate): # with probability `bad_request_rate` create a bad request
            print('[processor] Creating a bad request')
            body = {'name': 'crashhhhhhhhhhh'}
        else: #create a valid request
            print('[processor] Creating a valid request')
            body = {'name': str(uuid.uuid4()), 
                'action': {'name': 'myAction', 'parameters': {'topic': 'demoTopic'}, 'type': 'KafkaTopic'},
                'filters': [{'key': 'data.api', 'type': 'StringEquals', 'value': 'PutBlockList'}],
                'transformationTemplate': '{\'api\': \'{data.api}\''
                }
    
        bridge = random.choice(bridges)
        response = requests.post(url + '/api/v1/bridges/' + bridge + '/processors', data=json.dumps(body),
                headers={'Content-type': 'application/json', 'Authorization': 'Bearer {0}'.format(get_bearer_token())})
        if (response.status_code == 401):
            authenticate()
            continue
        elif(response.status_code == 202):
            processors.append(json.loads(response.text)['id'])
        print('[processor] response status code: {0}'.format(response.status_code))
        print('[processor] sleeping for 20 seconds')
        time.sleep(20)

def ingress():
    while True:
        if (len(processors) == 0):
            continue

        if (random.random() < bad_request_rate): # with probability `bad_request_rate` create a bad request
            print('[ingress] Creating a bad request')
            body = {'name': 'crashhhhhhhhhhh with a non valid cloud event'}
        else: #create a valid request
            print('[ingress] Creating a valid request')
            body = get_cloud_event()
    
        bridge = random.choice(bridges)
        response = requests.post(url + '/ingress/events/' + bridge, data=json.dumps(body), 
                headers={'Content-type': 'application/json', 'Authorization': 'Bearer {0}'.format(get_bearer_token())})
        if (response.status_code == 401):
            authenticate()
        else:
            print('[ingress] response status code: {0}'.format(response.status_code))
            time.sleep(10)
            print('[ingress] sleeping for 10 seconds')

def authenticate():
    global bearer_token
    print('[auth] Authentication in progress')
    token = requests.post(keycloak + '/auth/realms/event-bridge-fm/protocol/openid-connect/token',
        data={'grant_type': 'password', 'username': username, 'password': password, 'client_id': 'event-bridge', 'client_secret': 'secret'},
        headers={'Content-type': 'application/x-www-form-urlencoded'})
    bearer_token = json.loads(token.text)['access_token']
    print('[auth] Authenticated!')

def get_bearer_token():
    global bearer_token
    return bearer_token

def get_cloud_event():
    # With probability `match_filter_rate` generate a data.api that is not PutBlockList
    cloud_event = {
        'specversion': '1.0',
        'type': 'Microsoft.Storage.BlobCreated',
        'source': 'mySource',
        'id': '9aeb0fdf-c01e-0131-0922-9eb54906e209',
        'time': '2019-11-18T15:13:39.4589254Z',
        'subject': 'blobServices/default/containers/{storage-container}/blobs/{new-file}',
        'dataschema': '#',
        'data': {
            'api': 'OtherOperation' if random.random() < match_filter_rate else 'PutBlockList',
            'clientRequestId': '4c5dd7fb-2c48-4a27-bb30-5361b5de920a',
            'requestId': '9aeb0fdf-c01e-0131-0922-9eb549000000',
            'eTag': '0x8D76C39E4407333',
            'contentType': 'image/png',
            'contentLength': 30699,
            'blobType': 'BlockBlob',
            'url': 'https://gridtesting.blob.core.windows.net/testcontainer/{new-file}',
            'sequencer': '000000000000000000000000000099240000000000c41c18',
            'storageDiagnostics': {
                'batchId': '681fe319-3006-00a8-0022-9e7cde000000'
            }
        }
    }
    return cloud_event


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Generate bridge traffic')
    parser.add_argument('--manager', help='The url of the manager', required=True)
    parser.add_argument('--keycloak', help='The keycloak url', required=True)
    parser.add_argument('--username', help='The username', required=True)
    parser.add_argument('--password', help='The password', required=True)
    parser.add_argument('--bad_request_rate', help='The bad request rate', type=float, required=True)
    parser.add_argument('--match_filter_rate', help='How many cloud events should match the filter (rate).', type=float, required=True)
    # Initialize global vars
    args = vars(parser.parse_args())
    url = args['manager']
    keycloak = args['keycloak']
    username = args['username']
    password = args['password']
    bad_request_rate = args['bad_request_rate']
    match_filter_rate = args['match_filter_rate']
    bearer_token = ''

    bridge_thread = threading.Thread(target=create_bridge, args=())
    bridge_thread.start() 

    processor_thread = threading.Thread(target=create_processor, args=())
    processor_thread.start() 

    ingress_thread = threading.Thread(target=ingress, args=())
    ingress_thread.start() 
