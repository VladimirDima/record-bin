import React, { Component } from 'react';
import { connect } from 'react-redux';

import { addFolder } from '../actions/directoryTree';
import Folder from '../components/Folder';

class Root extends Component {

    addFolder = (folderName, path=[]) => this.props.addFolder(
        this.props.directoryTree,
        path,
        folderName
    );

    renderFolders = () => this.props.directoryTree.folders.map(folder => (
        <li key={folder.id}>
            <Folder addFolder={this.addFolder} {...folder}/>
        </li>
    ));

    render() {
        return <ul>{ this.renderFolders() }</ul>;
    }
}

const mapStateToProps = state => ({ directoryTree: state.directoryTree });

export default connect(mapStateToProps, { addFolder })(Root);