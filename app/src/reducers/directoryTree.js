import _ from 'lodash';
import uuidv4 from 'uuid/v4';

import {
    ADD_FOLDER,
    ADD_TRACK,
    DELETE_FOLDER,
    DELETE_TRACK,
    MOVE_TRACK,
} from '../actions/directoryTree';

const getNode = (root, path, depth=0) => depth >= path.length
    ? root
    : getNode(
        _.find(root.folders, folder => folder.id === path[depth]),
        path,
        depth + 1
    );

const getParentNode = (root, path, depth=0) => depth >= path.length - 1
    ? root
    : getParentNode(
        _.find(root.folders, folder => folder.id === path[depth]),
        path,
        depth + 1
    );


const deepCopy = object => JSON.parse(JSON.stringify(object));

const addFolder = (state, action) => {
    const tree = deepCopy(state);
    const folder = getNode(tree, action.path);
    folder.folders.push({
        id: `folder-${uuidv4()}`,
        name: action.folderName,
        folders: [],
        tracks: [],
    });

    return tree;
};

const deleteFolder = (state, action) => {
    const tree = deepCopy(state);
    const parent = getParentNode(tree, action.path);
    parent.folders = parent.folders.filter(
        folder => folder.id !== action.path[action.path.length - 1]
    );

    return tree;
};

const addTrack = (state, action) => {
    const tree = deepCopy(state);
    const folder = getNode(tree, action.path);
    folder.tracks.push(action.track);

    return tree;
};

const deleteTrack = (state, action) => {
    const tree = deepCopy(state);
    const folder = getNode(tree, action.path);
    folder.tracks = folder.tracks.filter(track => track.id !== action.trackId);

    return tree;
};

const getTrack = (state, action) => state;

const moveTrack = (state, action) => state;

const directoryTree = (state={
    id: 'root',
    name: 'root',
    folders: [],
    tracks: [],
}, action) => {
    switch (action.type) {
        case ADD_FOLDER:
            return addFolder(state, action);
        case DELETE_FOLDER:
            return deleteFolder(state, action);
        case ADD_TRACK:
            return addTrack(state, action);
        case DELETE_TRACK:
            return deleteTrack(state, action);
        case MOVE_TRACK:
            return moveTrack(state, action);
        default:
            return state;
    }
};

export default directoryTree;